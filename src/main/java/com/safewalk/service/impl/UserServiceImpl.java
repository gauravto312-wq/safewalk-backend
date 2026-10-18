package com.safewalk.service.impl;

import com.safewalk.dto.request.UserCreateRequest;
import com.safewalk.dto.response.UserResponse;
import com.safewalk.entity.User;
import com.safewalk.exception.DuplicateResourceException;
import com.safewalk.exception.ResourceNotFoundException;
import com.safewalk.mapper.UserMapper;
import com.safewalk.repository.UserRepository;
import com.safewalk.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class UserServiceImpl implements UserService {

    private static final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);

    private static final long MAX_AVATAR_SIZE = 5 * 1024 * 1024; // 5MB
    private static final List<String> ALLOWED_AVATAR_EXTENSIONS = Arrays.asList("jpg", "jpeg", "png", "webp");

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final Path avatarUploadLocation;

    public UserServiceImpl(UserRepository userRepository, UserMapper userMapper) {
        this(userRepository, userMapper, null, null);
    }

    public UserServiceImpl(UserRepository userRepository, UserMapper userMapper, PasswordEncoder passwordEncoder) {
        this(userRepository, userMapper, passwordEncoder, null);
    }

    @Autowired
    public UserServiceImpl(
            UserRepository userRepository,
            UserMapper userMapper,
            @Autowired(required = false) PasswordEncoder passwordEncoder,
            @Value("${safewalk.upload.avatar-dir:${safewalk.storage.base-dir:./uploads}/avatars}") String avatarUploadDir) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.avatarUploadLocation = Paths.get(avatarUploadDir != null ? avatarUploadDir : "./uploads/avatars").toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.avatarUploadLocation);
        } catch (IOException e) {
            log.error("Could not initialize avatar storage directory at {}", this.avatarUploadLocation, e);
        }
    }

    @Override
    @Transactional
    public UserResponse createUser(UserCreateRequest request) {
        String cleanMobileNumber = request.getMobileNumber().trim();
        log.info("Attempting to create user with mobile number: {}", cleanMobileNumber);

        if (userRepository.existsByMobileNumber(cleanMobileNumber)) {
            log.warn("Mobile number registration collision for: {}", cleanMobileNumber);
            throw new DuplicateResourceException("Mobile number '" + cleanMobileNumber + "' is already registered with a SafeWalk account.");
        }

        User user = userMapper.toEntity(request);
        if (passwordEncoder != null && request.getPassword() != null && !request.getPassword().trim().isEmpty()) {
            user.setPassword(passwordEncoder.encode(request.getPassword().trim()));
        }
        if (passwordEncoder != null && request.getEmergencyPassword() != null && !request.getEmergencyPassword().trim().isEmpty()) {
            user.setEmergencyPassword(passwordEncoder.encode(request.getEmergencyPassword().trim()));
        }
        if (request.getRole() != null) {
            user.setRole(request.getRole());
        }

        User savedUser = userRepository.save(user);

        log.info("Successfully created SafeWalk user with ID: {} and mobile number: {}", savedUser.getId(), savedUser.getMobileNumber());
        return userMapper.toResponse(savedUser);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserById(Long id) {
        log.debug("Fetching user by ID: {}", id);
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + id));
        return userMapper.toResponse(user);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserByMobileNumber(String mobileNumber) {
        log.debug("Fetching user by mobile number: {}", mobileNumber);
        User user = userRepository.findByMobileNumber(mobileNumber.trim())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with mobile number: " + mobileNumber));
        return userMapper.toResponse(user);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserResponse> getAllUsers() {
        log.debug("Fetching all users");
        return userRepository.findAll().stream()
                .map(userMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public UserResponse uploadProfilePicture(String mobileNumber, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Profile picture file cannot be null or empty.");
        }

        if (file.getSize() > MAX_AVATAR_SIZE) {
            throw new IllegalArgumentException("Profile picture size exceeds maximum limit of 5MB.");
        }

        String originalFilename = StringUtils.cleanPath(file.getOriginalFilename() != null ? file.getOriginalFilename() : "avatar.jpg");
        String extension = getFileExtension(originalFilename);

        if (!ALLOWED_AVATAR_EXTENSIONS.contains(extension.toLowerCase())) {
            throw new IllegalArgumentException("Invalid image file type ." + extension + ". Allowed formats: " + ALLOWED_AVATAR_EXTENSIONS);
        }

        User user = userRepository.findByMobileNumber(mobileNumber.trim())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with mobile number: " + mobileNumber));

        String storedFilename = "avatar_" + user.getId() + "_" + UUID.randomUUID() + "." + extension;
        Path targetLocation = this.avatarUploadLocation.resolve(storedFilename);

        try {
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            log.error("Failed to store avatar file for user ID {}", user.getId(), e);
            throw new RuntimeException("Failed to store profile picture: " + e.getMessage());
        }

        String avatarUrl = "/api/v1/users/" + user.getId() + "/avatar";
        user.setAvatarUrl(avatarUrl);
        User savedUser = userRepository.save(user);

        log.info("Successfully updated avatar for user ID: {} with path: {}", user.getId(), avatarUrl);
        return userMapper.toResponse(savedUser);
    }

    @Override
    @Transactional(readOnly = true)
    public Resource getProfilePictureResource(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

        if (user.getAvatarUrl() == null || user.getAvatarUrl().trim().isEmpty()) {
            throw new ResourceNotFoundException("User has not configured a profile picture.");
        }

        try {
            try (var stream = Files.list(this.avatarUploadLocation)) {
                Optional<Path> foundPath = stream
                        .filter(p -> p.getFileName().toString().startsWith("avatar_" + userId + "_"))
                        .findFirst();

                if (foundPath.isPresent()) {
                    Resource resource = new UrlResource(foundPath.get().toUri());
                    if (resource.exists() && resource.isReadable()) {
                        return resource;
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error reading avatar file for user ID {}", userId, e);
        }

        throw new ResourceNotFoundException("Avatar file not found on server for user ID: " + userId);
    }

    private String getFileExtension(String filename) {
        if (filename == null || filename.lastIndexOf(".") == -1) {
            return "jpg";
        }
        return filename.substring(filename.lastIndexOf(".") + 1);
    }
}

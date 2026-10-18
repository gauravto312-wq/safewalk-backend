package com.safewalk.service;

import com.safewalk.dto.request.UserCreateRequest;
import com.safewalk.dto.response.UserResponse;
import com.safewalk.entity.AccountStatus;
import com.safewalk.entity.User;
import com.safewalk.exception.DuplicateResourceException;
import com.safewalk.exception.ResourceNotFoundException;
import com.safewalk.mapper.UserMapper;
import com.safewalk.repository.UserRepository;
import com.safewalk.service.impl.UserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    private UserMapper userMapper;
    private UserServiceImpl userService;

    private UserCreateRequest createRequest;
    private User sampleUser;

    @BeforeEach
    void setUp() {
        userMapper = new UserMapper();
        userService = new UserServiceImpl(userRepository, userMapper);

        createRequest = UserCreateRequest.builder()
                .name("Aman Sharma")
                .mobileNumber("+919876543210")
                .password("secret123")
                .emergencyPassword("sosPass123")
                .build();

        sampleUser = User.builder()
                .id(1L)
                .name("Aman Sharma")
                .mobileNumber("+919876543210")
                .password("secret123")
                .emergencyPassword("sosPass123")
                .accountStatus(AccountStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    void createUser_Success() {
        when(userRepository.existsByMobileNumber(createRequest.getMobileNumber())).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(sampleUser);

        UserResponse response = userService.createUser(createRequest);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("Aman Sharma", response.getName());
        assertEquals("+919876543210", response.getMobileNumber());
        assertEquals(AccountStatus.ACTIVE, response.getAccountStatus());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void createUser_DuplicateMobileNumber_ThrowsException() {
        when(userRepository.existsByMobileNumber(createRequest.getMobileNumber())).thenReturn(true);

        DuplicateResourceException exception = assertThrows(
                DuplicateResourceException.class,
                () -> userService.createUser(createRequest)
        );

        assertEquals("Mobile number '+919876543210' is already registered with a SafeWalk account.", exception.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void getUserById_Success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));

        UserResponse response = userService.getUserById(1L);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("Aman Sharma", response.getName());
    }

    @Test
    void getUserById_NotFound_ThrowsException() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> userService.getUserById(999L));
    }
}

package com.safewalk.service;

import com.safewalk.dto.request.UserCreateRequest;
import com.safewalk.dto.response.UserResponse;

import java.util.List;

public interface UserService {

    UserResponse createUser(UserCreateRequest request);

    UserResponse getUserById(Long id);

    UserResponse getUserByMobileNumber(String mobileNumber);

    List<UserResponse> getAllUsers();

    UserResponse uploadProfilePicture(String mobileNumber, org.springframework.web.multipart.MultipartFile file);

    org.springframework.core.io.Resource getProfilePictureResource(Long userId);
}

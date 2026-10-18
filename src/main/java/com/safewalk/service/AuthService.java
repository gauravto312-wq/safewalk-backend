package com.safewalk.service;

import com.safewalk.dto.request.LoginRequest;
import com.safewalk.dto.request.RegisterRequest;
import com.safewalk.dto.response.AuthResponse;
import com.safewalk.dto.response.UserResponse;

public interface AuthService {

    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    UserResponse getCurrentUserProfile(String mobileNumber);
}

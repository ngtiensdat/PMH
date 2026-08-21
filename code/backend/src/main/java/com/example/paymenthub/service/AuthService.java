package com.example.paymenthub.service;

import com.example.paymenthub.dto.request.LoginRequest;
import com.example.paymenthub.dto.response.LoginResponse;

public interface AuthService {
    LoginResponse login(LoginRequest request);
    LoginResponse getCurrentUser(String username);
}

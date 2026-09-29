package com.futureboundtech.service;

import com.futureboundtech.dto.PasswordResetDto;
import com.futureboundtech.dto.UserRegistrationDto;

public interface AuthService {
    void registerStudent(UserRegistrationDto registrationDto) throws RuntimeException;
    void createPasswordResetTokenForUser(String email) throws RuntimeException;
    void resetPassword(PasswordResetDto resetDto) throws RuntimeException;
}

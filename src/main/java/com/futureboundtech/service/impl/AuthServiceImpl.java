package com.futureboundtech.service.impl;

import com.futureboundtech.dto.PasswordResetDto;
import com.futureboundtech.dto.UserRegistrationDto;
import com.futureboundtech.entity.PasswordResetToken;
import com.futureboundtech.entity.Student;
import com.futureboundtech.entity.User;
import com.futureboundtech.enums.Role;
import com.futureboundtech.exception.BusinessException;
import com.futureboundtech.repository.PasswordResetTokenRepository;
import com.futureboundtech.repository.UserRepository;
import com.futureboundtech.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void registerStudent(UserRegistrationDto dto) throws RuntimeException {
        if (userRepository.findByEmail(dto.getEmail()).isPresent()) {
            throw new BusinessException("Email already exists");
        }
        if (!dto.getPassword().equals(dto.getConfirmPassword())) {
            throw new BusinessException("Passwords do not match");
        }

        String phone = dto.getPhone() == null ? null : dto.getPhone().trim();
        if (phone != null && !phone.isEmpty() && userRepository.existsByPhone(phone)) {
            throw new BusinessException("This phone number is already registered. Please use a different phone number or log in.");
        }

        User user = User.builder()
                .firstName(dto.getFirstName())
                .lastName(dto.getLastName())
                .email(dto.getEmail())
                .phone(phone)
                .password(passwordEncoder.encode(dto.getPassword()))
                .role(Role.STUDENT)
                .active(true)
                .build();
                
        Student studentProfile = new Student();
        studentProfile.setUser(user);
        user.setStudentProfile(studentProfile);

        userRepository.save(user);
    }

    @Override
    @Transactional
    public void createPasswordResetTokenForUser(String email) throws RuntimeException {
        // SECURITY (Phase 25): do NOT reveal whether an address is registered. When the
        // user is unknown we silently succeed so the controller can show the same generic
        // "if an account exists, a link was sent" message and block email enumeration.
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            log.debug("Password reset requested for unknown email (no token issued).");
            return;
        }

        tokenRepository.deleteByUser(user); // clear old tokens

        String token = UUID.randomUUID().toString();
        PasswordResetToken resetToken = PasswordResetToken.builder()
                .token(token)
                .user(user)
                .expiryDate(LocalDateTime.now().plusHours(24))
                .build();

        tokenRepository.save(resetToken);

        // Email delivery is disabled in this build, so the link is logged for the operator.
        // Kept at DEBUG level so reset tokens never land in production INFO logs.
        // In a real deployment send the reset e-mail here instead.
        log.debug("Password reset link generated for {} (token issued, not logged).", email);
    }

    @Override
    @Transactional
    public void resetPassword(PasswordResetDto resetDto) throws RuntimeException {
        if (!resetDto.getNewPassword().equals(resetDto.getConfirmPassword())) {
            throw new RuntimeException("Passwords do not match");
        }
        
        PasswordResetToken resetToken = tokenRepository.findByToken(resetDto.getToken())
                .orElseThrow(() -> new RuntimeException("Invalid token"));
                
        if (resetToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Token has expired");
        }
        
        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(resetDto.getNewPassword()));
        userRepository.save(user);
        
        tokenRepository.delete(resetToken);
    }
}

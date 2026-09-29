package com.futureboundtech.unit;

import com.futureboundtech.dto.PasswordResetDto;
import com.futureboundtech.dto.UserRegistrationDto;
import com.futureboundtech.entity.PasswordResetToken;
import com.futureboundtech.entity.User;
import com.futureboundtech.enums.Role;
import com.futureboundtech.exception.BusinessException;
import com.futureboundtech.repository.PasswordResetTokenRepository;
import com.futureboundtech.repository.UserRepository;
import com.futureboundtech.service.impl.AuthServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link AuthServiceImpl}: registration validation, BCrypt
 * hashing and the Phase 25 anti-enumeration / token-handling behaviour.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuthServiceImplTest {

    @Mock UserRepository userRepository;
    @Mock PasswordResetTokenRepository tokenRepository;
    @Mock PasswordEncoder passwordEncoder;

    @InjectMocks AuthServiceImpl authService;

    @Captor ArgumentCaptor<User> userCaptor;

    private static UserRegistrationDto registration(String email, String pw, String confirm) {
        UserRegistrationDto dto = new UserRegistrationDto();
        dto.setFirstName("Ada");
        dto.setLastName("Lovelace");
        dto.setEmail(email);
        dto.setPhone("9998887777");
        dto.setPassword(pw);
        dto.setConfirmPassword(confirm);
        return dto;
    }

    @Test
    @DisplayName("registering an address that already exists is rejected")
    void duplicateEmail() {
        when(userRepository.findByEmail("a@b.com")).thenReturn(Optional.of(new User()));
        assertThrows(BusinessException.class,
                () -> authService.registerStudent(registration("a@b.com", "secret123", "secret123")));
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("mismatched passwords are rejected before anything is saved")
    void passwordMismatch() {
        when(userRepository.findByEmail("a@b.com")).thenReturn(Optional.empty());
        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.registerStudent(registration("a@b.com", "secret123", "different")));
        assertTrue(ex.getMessage().toLowerCase().contains("password"));
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("a phone number already on file is rejected")
    void duplicatePhone() {
        when(userRepository.findByEmail("a@b.com")).thenReturn(Optional.empty());
        when(userRepository.existsByPhone("9998887777")).thenReturn(true);
        assertThrows(BusinessException.class,
                () -> authService.registerStudent(registration("a@b.com", "secret123", "secret123")));
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("successful registration hashes the password and creates a STUDENT with a profile")
    void successfulRegistration() {
        when(userRepository.findByEmail("a@b.com")).thenReturn(Optional.empty());
        when(userRepository.existsByPhone("9998887777")).thenReturn(false);
        when(passwordEncoder.encode("secret123")).thenReturn("$2a$10$HASHED");

        authService.registerStudent(registration("a@b.com", "secret123", "secret123"));

        verify(userRepository).save(userCaptor.capture());
        User saved = userCaptor.getValue();
        assertEquals(Role.STUDENT, saved.getRole());
        assertEquals("$2a$10$HASHED", saved.getPassword());   // raw password never stored
        assertTrue(saved.isActive());
        assertNotNull(saved.getStudentProfile());
        assertSame(saved, saved.getStudentProfile().getUser());
    }

    @Test
    @DisplayName("requesting a reset for an unknown email issues no token (no enumeration)")
    void resetTokenForUnknownEmailIsSilent() {
        when(userRepository.findByEmail("ghost@nowhere.com")).thenReturn(Optional.empty());
        authService.createPasswordResetTokenForUser("ghost@nowhere.com");
        verify(tokenRepository, never()).save(any());
        verify(tokenRepository, never()).deleteByUser(any());
    }

    @Test
    @DisplayName("requesting a reset for a known email clears old tokens and issues one")
    void resetTokenForKnownEmail() {
        User user = User.builder().email("a@b.com").role(Role.STUDENT).build();
        when(userRepository.findByEmail("a@b.com")).thenReturn(Optional.of(user));

        authService.createPasswordResetTokenForUser("a@b.com");

        verify(tokenRepository).deleteByUser(user);
        ArgumentCaptor<PasswordResetToken> captor = ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(tokenRepository).save(captor.capture());
        assertNotNull(captor.getValue().getToken());
        assertSame(user, captor.getValue().getUser());
    }

    @Test
    @DisplayName("reset with mismatched confirmation is rejected")
    void resetPasswordMismatch() {
        PasswordResetDto dto = new PasswordResetDto();
        dto.setToken("t");
        dto.setNewPassword("newpass123");
        dto.setConfirmPassword("other12345");
        assertThrows(RuntimeException.class, () -> authService.resetPassword(dto));
    }

    @Test
    @DisplayName("reset with an unknown token is rejected")
    void resetPasswordInvalidToken() {
        PasswordResetDto dto = new PasswordResetDto();
        dto.setToken("missing");
        dto.setNewPassword("newpass123");
        dto.setConfirmPassword("newpass123");
        when(tokenRepository.findByToken("missing")).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> authService.resetPassword(dto));
    }

    @Test
    @DisplayName("reset with an expired token is rejected")
    void resetPasswordExpiredToken() {
        PasswordResetDto dto = new PasswordResetDto();
        dto.setToken("expired");
        dto.setNewPassword("newpass123");
        dto.setConfirmPassword("newpass123");
        PasswordResetToken token = PasswordResetToken.builder()
                .token("expired").expiryDate(LocalDateTime.now().minusHours(1)).build();
        when(tokenRepository.findByToken("expired")).thenReturn(Optional.of(token));
        assertThrows(RuntimeException.class, () -> authService.resetPassword(dto));
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("a valid, unexpired single-use token resets the password and is consumed")
    void resetPasswordSuccess() {
        PasswordResetDto dto = new PasswordResetDto();
        dto.setToken("good");
        dto.setNewPassword("newpass123");
        dto.setConfirmPassword("newpass123");
        User user = User.builder().email("a@b.com").role(Role.STUDENT).build();
        PasswordResetToken token = PasswordResetToken.builder()
                .token("good").user(user).expiryDate(LocalDateTime.now().plusHours(1)).build();
        when(tokenRepository.findByToken("good")).thenReturn(Optional.of(token));
        when(passwordEncoder.encode("newpass123")).thenReturn("$2a$10$NEWHASH");

        authService.resetPassword(dto);

        assertEquals("$2a$10$NEWHASH", user.getPassword());
        verify(userRepository).save(user);
        verify(tokenRepository).delete(token);   // token invalidated after use
    }
}

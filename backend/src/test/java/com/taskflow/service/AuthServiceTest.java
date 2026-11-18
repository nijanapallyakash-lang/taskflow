package com.taskflow.service;

import com.taskflow.dto.Dtos.LoginRequest;
import com.taskflow.dto.Dtos.RegisterRequest;
import com.taskflow.model.User;
import com.taskflow.repo.UserRepository;
import com.taskflow.security.JwtService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {
    @Mock UserRepository users;
    @Spy PasswordEncoder encoder = new BCryptPasswordEncoder(4);
    @Mock JwtService jwt;
    @Mock MailService mail;
    @InjectMocks AuthService auth;

    @Test
    void registerHashesPasswordAndSendsVerificationLink() {
        when(mail.link(anyString())).thenAnswer(i -> "http://app" + i.getArgument(0));

        auth.register(new RegisterRequest("New@Dev.io", "secret123", "New Dev"));

        verify(users).save(argThat(u -> u.getEmail().equals("new@dev.io")
                && !u.getPassword().equals("secret123") && encoder.matches("secret123", u.getPassword())
                && !u.isVerified() && u.getVerificationToken() != null && u.getRole() == User.Role.TEAM_MEMBER));
        verify(mail).send(eq("new@dev.io"), anyString(), contains("/verify?token="));
    }

    @Test
    void duplicateEmailIsConflict() {
        when(users.existsByEmailIgnoreCase("a@b.io")).thenReturn(true);
        assertThatThrownBy(() -> auth.register(new RegisterRequest("a@b.io", "secret123", "A")))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("already registered");
    }

    @Test
    void unverifiedUserCannotLogIn() {
        User u = new User();
        u.setEmail("a@b.io");
        u.setPassword(encoder.encode("secret123"));
        when(users.findByEmailIgnoreCase("a@b.io")).thenReturn(Optional.of(u));

        assertThatThrownBy(() -> auth.login(new LoginRequest("a@b.io", "secret123")))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("verify");
    }

    @Test
    void wrongPasswordIsUnauthorized() {
        User u = new User();
        u.setPassword(encoder.encode("secret123"));
        u.setVerified(true);
        when(users.findByEmailIgnoreCase("a@b.io")).thenReturn(Optional.of(u));

        assertThatThrownBy(() -> auth.login(new LoginRequest("a@b.io", "nope")))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("Invalid email or password");
    }

    @Test
    void verifiedUserGetsToken() {
        User u = new User();
        u.setPassword(encoder.encode("secret123"));
        u.setVerified(true);
        when(users.findByEmailIgnoreCase("a@b.io")).thenReturn(Optional.of(u));
        when(jwt.generate(u)).thenReturn("jwt-token");

        assertThat(auth.login(new LoginRequest("a@b.io", "secret123")).token()).isEqualTo("jwt-token");
    }
}

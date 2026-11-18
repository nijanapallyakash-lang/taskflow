package com.taskflow.service;

import com.taskflow.dto.Dtos.*;
import com.taskflow.model.User;
import com.taskflow.repo.UserRepository;
import com.taskflow.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

import static com.taskflow.web.ApiExceptionHandler.badRequest;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {
    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final JwtService jwt;
    private final MailService mail;

    public UserDto register(RegisterRequest r) {
        String email = r.email().trim().toLowerCase();
        if (users.existsByEmailIgnoreCase(email))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email is already registered");
        User u = new User();
        u.setEmail(email);
        u.setPassword(encoder.encode(r.password()));
        u.setFullName(r.fullName().trim());
        u.setRole(User.Role.TEAM_MEMBER);
        u.setVerificationToken(UUID.randomUUID().toString());
        users.save(u);
        mail.send(email, "Verify your TaskFlow account",
                "Hi " + u.getFullName() + ",\n\nConfirm your email to activate your account:\n"
                        + mail.link("/verify?token=" + u.getVerificationToken()) + "\n\n- TaskFlow");
        return UserDto.from(u);
    }

    public void verify(String token) {
        User u = users.findByVerificationToken(token)
                .orElseThrow(() -> badRequest("Invalid or already used verification link"));
        u.setVerified(true);
        u.setVerificationToken(null);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest r) {
        User u = users.findByEmailIgnoreCase(r.email().trim())
                .filter(x -> encoder.matches(r.password(), x.getPassword()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password"));
        if (!u.isVerified()) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Please verify your email first");
        if (!u.isActive()) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Your account has been deactivated");
        return new AuthResponse(jwt.generate(u), UserDto.from(u));
    }
}

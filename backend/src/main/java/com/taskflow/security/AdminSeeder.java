package com.taskflow.security;

import com.taskflow.model.User;
import com.taskflow.repo.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/** Creates the first admin from ADMIN_EMAIL / ADMIN_PASSWORD when no admin exists. */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminSeeder implements CommandLineRunner {
    private final UserRepository users;
    private final PasswordEncoder encoder;
    @Value("${app.admin-email}")
    private String email;
    @Value("${app.admin-password}")
    private String password;

    @Override
    public void run(String... args) {
        if (users.existsByRole(User.Role.ADMIN)) return;
        User u = new User();
        u.setEmail(email.toLowerCase());
        u.setPassword(encoder.encode(password));
        u.setFullName("Administrator");
        u.setRole(User.Role.ADMIN);
        u.setVerified(true);
        users.save(u);
        log.info("Seeded admin account {}", email);
    }
}

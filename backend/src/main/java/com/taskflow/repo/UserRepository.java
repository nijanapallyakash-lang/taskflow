package com.taskflow.repo;

import com.taskflow.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmailIgnoreCase(String email);
    Optional<User> findByVerificationToken(String token);
    boolean existsByEmailIgnoreCase(String email);
    boolean existsByRole(User.Role role);
}

package com.taskflow.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@Setter
public class User {
    public enum Role { ADMIN, PROJECT_MANAGER, TEAM_MEMBER }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String email;
    private String password;
    private String fullName;
    @Enumerated(EnumType.STRING)
    private Role role = Role.TEAM_MEMBER;
    private boolean verified;
    private boolean active = true;
    private String verificationToken;
    private String avatarPath;
    private String skills;
    private LocalDateTime createdAt = LocalDateTime.now();

    // The JWT principal is loaded outside the request's persistence context, so compare by id.
    @Override
    public boolean equals(Object o) {
        return o instanceof User u && id != null && id.equals(u.getId());
    }

    @Override
    public int hashCode() {
        return User.class.hashCode();
    }
}

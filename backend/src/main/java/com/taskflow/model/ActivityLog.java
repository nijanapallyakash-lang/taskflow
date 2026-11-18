package com.taskflow.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "activity_logs")
@Getter
@Setter
public class ActivityLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false)
    private Project project;
    @ManyToOne
    private Task task;
    @ManyToOne(optional = false)
    private User user;
    private String action;
    private String details;
    private LocalDateTime createdAt = LocalDateTime.now();
}

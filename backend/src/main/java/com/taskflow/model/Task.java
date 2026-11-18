package com.taskflow.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "tasks")
@Getter
@Setter
public class Task {
    public enum Type { BUG, TASK, STORY }
    public enum Priority { LOW, MEDIUM, HIGH, CRITICAL }
    public enum Status { TODO, IN_PROGRESS, CODE_REVIEW, DONE }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false)
    private Project project;
    @ManyToOne
    private Sprint sprint;
    @ManyToOne
    private Task parent;
    private String title;
    private String description;
    @Enumerated(EnumType.STRING)
    private Type type = Type.TASK;
    @Enumerated(EnumType.STRING)
    private Priority priority = Priority.MEDIUM;
    @Enumerated(EnumType.STRING)
    private Status status = Status.TODO;
    @ManyToOne
    private User assignee;
    @ManyToOne(optional = false)
    private User reporter;
    private String labels;
    private Double estimatedHours;
    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime updatedAt = LocalDateTime.now();
    private LocalDateTime completedAt;
}

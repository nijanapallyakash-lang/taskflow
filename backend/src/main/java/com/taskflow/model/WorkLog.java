package com.taskflow.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "work_logs")
@Getter
@Setter
public class WorkLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false)
    private Task task;
    @ManyToOne(optional = false)
    private User user;
    private double hours;
    private String note;
    private LocalDate logDate;
    private LocalDateTime createdAt = LocalDateTime.now();
}

package com.taskflow.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "sprints")
@Getter
@Setter
public class Sprint {
    public enum Status { PLANNED, ACTIVE, COMPLETED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false)
    private Project project;
    private String name;
    private String goal;
    @Enumerated(EnumType.STRING)
    private Status status = Status.PLANNED;
    private LocalDate startDate;
    private LocalDate endDate;
}

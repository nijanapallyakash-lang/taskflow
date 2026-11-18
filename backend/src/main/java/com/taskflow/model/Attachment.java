package com.taskflow.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "attachments")
@Getter
@Setter
public class Attachment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false)
    private Task task;
    private String fileName;
    private String storedName;
    private String contentType;
    private long size;
    @ManyToOne(optional = false)
    private User uploadedBy;
    private LocalDateTime createdAt = LocalDateTime.now();
}

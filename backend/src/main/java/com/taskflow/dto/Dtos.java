package com.taskflow.dto;

import com.taskflow.model.*;
import jakarta.validation.constraints.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/** Request/response shapes for the REST API. Entities never leave the service layer as JSON. */
public final class Dtos {
    private Dtos() {}

    // ---- auth & users ----
    public record RegisterRequest(@NotBlank @Email String email,
                                  @NotBlank @Size(min = 8, max = 100) String password,
                                  @NotBlank @Size(max = 120) String fullName) {}

    public record LoginRequest(@NotBlank @Email String email, @NotBlank String password) {}

    public record AuthResponse(String token, UserDto user) {}

    public record UserDto(Long id, String email, String fullName, User.Role role, boolean active,
                          boolean verified, String skills, boolean hasAvatar) {
        public static UserDto from(User u) {
            return u == null ? null : new UserDto(u.getId(), u.getEmail(), u.getFullName(), u.getRole(),
                    u.isActive(), u.isVerified(), u.getSkills(), u.getAvatarPath() != null);
        }
    }

    public record ProfileUpdate(@NotBlank @Size(max = 120) String fullName, @Size(max = 1000) String skills) {}

    public record PasswordChange(@NotBlank String currentPassword, @NotBlank @Size(min = 8, max = 100) String newPassword) {}

    public record AdminUserUpdate(Boolean active, User.Role role) {}

    // ---- projects & sprints ----
    public record ProjectRequest(@NotBlank @Size(max = 150) String name, @Size(max = 5000) String description) {}

    public record MemberRequest(@NotBlank @Email String email) {}

    public record ProjectDto(Long id, String name, String description, boolean archived, UserDto owner, List<UserDto> members) {
        public static ProjectDto from(Project p) {
            return new ProjectDto(p.getId(), p.getName(), p.getDescription(), p.isArchived(), UserDto.from(p.getOwner()),
                    p.getMembers().stream().sorted(Comparator.comparing(User::getFullName)).map(UserDto::from).toList());
        }
    }

    public record SprintRequest(@NotBlank @Size(max = 150) String name, @Size(max = 2000) String goal,
                                LocalDate startDate, LocalDate endDate) {}

    public record SprintDto(Long id, Long projectId, String name, String goal, Sprint.Status status,
                            LocalDate startDate, LocalDate endDate) {
        public static SprintDto from(Sprint s) {
            return new SprintDto(s.getId(), s.getProject().getId(), s.getName(), s.getGoal(), s.getStatus(),
                    s.getStartDate(), s.getEndDate());
        }
    }

    // ---- tasks ----
    public record TaskRequest(@NotBlank @Size(max = 255) String title,
                              @Size(max = 20000) String description,
                              @NotNull Task.Type type,
                              @NotNull Task.Priority priority,
                              Long assigneeId,
                              Long sprintId,
                              @Size(max = 500) String labels,
                              @PositiveOrZero Double estimatedHours) {}

    public record StatusUpdate(@NotNull Task.Status status) {}

    public record TaskDto(Long id, Long projectId, Long sprintId, Long parentId, String title, String description,
                          Task.Type type, Task.Priority priority, Task.Status status, UserDto assignee,
                          UserDto reporter, String labels, Double estimatedHours,
                          LocalDateTime createdAt, LocalDateTime updatedAt, LocalDateTime completedAt) {
        public static TaskDto from(Task t) {
            return new TaskDto(t.getId(), t.getProject().getId(), t.getSprint() == null ? null : t.getSprint().getId(),
                    t.getParent() == null ? null : t.getParent().getId(), t.getTitle(), t.getDescription(),
                    t.getType(), t.getPriority(), t.getStatus(), UserDto.from(t.getAssignee()),
                    UserDto.from(t.getReporter()), t.getLabels(), t.getEstimatedHours(),
                    t.getCreatedAt(), t.getUpdatedAt(), t.getCompletedAt());
        }
    }

    public record ChecklistRequest(@NotBlank @Size(max = 500) String text) {}

    public record ChecklistDto(Long id, String text, boolean done) {
        public static ChecklistDto from(ChecklistItem c) { return new ChecklistDto(c.getId(), c.getText(), c.isDone()); }
    }

    public record CommentRequest(@NotBlank @Size(max = 5000) String body) {}

    public record CommentDto(Long id, UserDto author, String body, LocalDateTime createdAt) {
        public static CommentDto from(Comment c) {
            return new CommentDto(c.getId(), UserDto.from(c.getAuthor()), c.getBody(), c.getCreatedAt());
        }
    }

    public record WorkLogRequest(@NotNull @Positive @Max(24) Double hours, @Size(max = 500) String note, LocalDate logDate) {}

    public record WorkLogDto(Long id, UserDto user, double hours, String note, LocalDate logDate) {
        public static WorkLogDto from(WorkLog w) {
            return new WorkLogDto(w.getId(), UserDto.from(w.getUser()), w.getHours(), w.getNote(), w.getLogDate());
        }
    }

    public record AttachmentDto(Long id, String fileName, String contentType, long size, UserDto uploadedBy, LocalDateTime createdAt) {
        public static AttachmentDto from(Attachment a) {
            return new AttachmentDto(a.getId(), a.getFileName(), a.getContentType(), a.getSize(),
                    UserDto.from(a.getUploadedBy()), a.getCreatedAt());
        }
    }

    public record ActivityDto(Long id, Long taskId, UserDto user, String action, String details, LocalDateTime createdAt) {
        public static ActivityDto from(ActivityLog a) {
            return new ActivityDto(a.getId(), a.getTask() == null ? null : a.getTask().getId(), UserDto.from(a.getUser()),
                    a.getAction(), a.getDetails(), a.getCreatedAt());
        }
    }

    public record TaskDetail(TaskDto task, List<TaskDto> subtasks, List<ChecklistDto> checklist,
                             List<AttachmentDto> attachments, List<CommentDto> comments,
                             List<WorkLogDto> workLogs, double loggedHours, List<ActivityDto> activity) {}

    // ---- dashboard ----
    public record BurndownPoint(LocalDate date, Long remaining, double ideal) {}

    public record Burndown(String sprintName, int totalTasks, List<BurndownPoint> points) {}

    public record DashboardDto(long totalTasks, long doneTasks, double completionPercent,
                               Map<Task.Status, Long> byStatus, Map<Task.Priority, Long> byPriority,
                               double estimatedHours, double loggedHours, Burndown burndown) {}
}

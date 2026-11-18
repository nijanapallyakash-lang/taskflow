package com.taskflow.service;

import com.taskflow.dto.Dtos.TaskRequest;
import com.taskflow.model.*;
import com.taskflow.repo.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {
    @Mock TaskRepository tasks;
    @Mock ChecklistItemRepository checklist;
    @Mock AttachmentRepository attachments;
    @Mock CommentRepository comments;
    @Mock WorkLogRepository workLogs;
    @Mock ActivityLogRepository activity;
    @Mock SprintRepository sprints;
    @Mock ProjectService projects;
    @Mock MailService mail;
    @Mock FileStorage storage;
    @InjectMocks TaskService service;

    User pm, dev, outsider;
    Project project;
    Task task;

    static User user(long id, User.Role role) {
        User u = new User();
        u.setId(id);
        u.setRole(role);
        u.setEmail("u" + id + "@test.dev");
        u.setFullName("User " + id);
        return u;
    }

    @BeforeEach
    void setUp() {
        pm = user(1, User.Role.PROJECT_MANAGER);
        dev = user(2, User.Role.TEAM_MEMBER);
        outsider = user(3, User.Role.TEAM_MEMBER);
        project = new Project();
        project.setId(10L);
        project.setName("Apollo");
        project.setOwner(pm);
        project.getMembers().add(pm);
        project.getMembers().add(dev);
        task = new Task();
        task.setId(100L);
        task.setProject(project);
        task.setTitle("Fix login");
        task.setReporter(pm);
        task.setAssignee(dev);
    }

    @Test
    void movingToDoneStampsCompletionAuditsAndNotifiesReporter() {
        when(tasks.findById(100L)).thenReturn(Optional.of(task));
        when(projects.write(10L, dev)).thenReturn(project);

        service.updateStatus(100L, Task.Status.DONE, dev);

        assertThat(task.getStatus()).isEqualTo(Task.Status.DONE);
        assertThat(task.getCompletedAt()).isNotNull();
        ArgumentCaptor<ActivityLog> log = ArgumentCaptor.forClass(ActivityLog.class);
        verify(activity).save(log.capture());
        assertThat(log.getValue().getAction()).isEqualTo("STATUS_CHANGED");
        assertThat(log.getValue().getDetails()).isEqualTo("TODO -> DONE");
        // actor (dev) is not emailed about their own change; reporter (pm) is
        verify(mail).send(eq(pm.getEmail()), contains("DONE"), anyString());
        verify(mail, never()).send(eq(dev.getEmail()), anyString(), anyString());
    }

    @Test
    void reopeningClearsCompletion() {
        task.setStatus(Task.Status.DONE);
        task.setCompletedAt(java.time.LocalDateTime.now());
        when(tasks.findById(100L)).thenReturn(Optional.of(task));

        service.updateStatus(100L, Task.Status.IN_PROGRESS, dev);

        assertThat(task.getCompletedAt()).isNull();
    }

    @Test
    void assigneeMustBeProjectMember() {
        when(projects.write(10L, pm)).thenReturn(project);
        TaskRequest r = new TaskRequest("New", null, Task.Type.BUG, Task.Priority.HIGH, outsider.getId(), null, null, 2.0);

        assertThatThrownBy(() -> service.create(10L, r, pm))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Assignee must be a project member");
        verify(tasks, never()).save(any());
    }

    @Test
    void createWithAssigneeEmailsThem() {
        when(projects.write(10L, pm)).thenReturn(project);
        TaskRequest r = new TaskRequest("New", null, Task.Type.STORY, Task.Priority.LOW, dev.getId(), null, "ui, auth", 3.0);

        Task created = service.create(10L, r, pm);

        assertThat(created.getAssignee()).isEqualTo(dev);
        assertThat(created.getLabels()).isEqualTo("ui, auth");
        verify(mail).send(eq(dev.getEmail()), contains("Assigned"), anyString());
    }

    @Test
    void teamMemberCannotDeleteSomeoneElsesTask() {
        when(tasks.findById(100L)).thenReturn(Optional.of(task));

        assertThatThrownBy(() -> service.delete(100L, dev)).isInstanceOf(ResponseStatusException.class);
        verify(tasks, never()).delete(any(Task.class));
    }
}

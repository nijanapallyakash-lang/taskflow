package com.taskflow.web;

import com.taskflow.dto.Dtos.*;
import com.taskflow.model.Attachment;
import com.taskflow.model.Task;
import com.taskflow.model.User;
import com.taskflow.service.TaskService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Tag(name = "Tasks")
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class TaskController {
    private final TaskService tasks;

    @GetMapping("/projects/{projectId}/tasks")
    public List<TaskDto> search(@AuthenticationPrincipal User me, @PathVariable Long projectId,
                                @RequestParam(required = false) Long assigneeId,
                                @RequestParam(required = false) Task.Priority priority,
                                @RequestParam(required = false) String label,
                                @RequestParam(required = false) Long sprintId,
                                @RequestParam(required = false) Boolean backlog,
                                @RequestParam(required = false) Task.Status status,
                                @RequestParam(required = false) String q) {
        return tasks.search(projectId, new TaskService.Filter(assigneeId, priority, label, sprintId, backlog, status, q), me)
                .stream().map(TaskDto::from).toList();
    }

    @PostMapping("/projects/{projectId}/tasks")
    @ResponseStatus(HttpStatus.CREATED)
    public TaskDto create(@AuthenticationPrincipal User me, @PathVariable Long projectId, @Valid @RequestBody TaskRequest r) {
        return TaskDto.from(tasks.create(projectId, r, me));
    }

    @GetMapping("/tasks/{id}")
    public TaskDetail detail(@AuthenticationPrincipal User me, @PathVariable Long id) {
        return tasks.detail(id, me);
    }

    @PutMapping("/tasks/{id}")
    public TaskDto update(@AuthenticationPrincipal User me, @PathVariable Long id, @Valid @RequestBody TaskRequest r) {
        return TaskDto.from(tasks.update(id, r, me));
    }

    @PatchMapping("/tasks/{id}/status")
    public TaskDto status(@AuthenticationPrincipal User me, @PathVariable Long id, @Valid @RequestBody StatusUpdate r) {
        return TaskDto.from(tasks.updateStatus(id, r.status(), me));
    }

    @DeleteMapping("/tasks/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal User me, @PathVariable Long id) {
        tasks.delete(id, me);
    }

    @PostMapping("/tasks/{id}/subtasks")
    @ResponseStatus(HttpStatus.CREATED)
    public TaskDto subtask(@AuthenticationPrincipal User me, @PathVariable Long id, @Valid @RequestBody TaskRequest r) {
        return TaskDto.from(tasks.createSubtask(id, r, me));
    }

    @PostMapping("/tasks/{id}/checklist")
    @ResponseStatus(HttpStatus.CREATED)
    public ChecklistDto addChecklist(@AuthenticationPrincipal User me, @PathVariable Long id, @Valid @RequestBody ChecklistRequest r) {
        return ChecklistDto.from(tasks.addChecklist(id, r.text(), me));
    }

    @PatchMapping("/checklist/{itemId}/toggle")
    public ChecklistDto toggle(@AuthenticationPrincipal User me, @PathVariable Long itemId) {
        return ChecklistDto.from(tasks.toggleChecklist(itemId, me));
    }

    @DeleteMapping("/checklist/{itemId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteChecklist(@AuthenticationPrincipal User me, @PathVariable Long itemId) {
        tasks.deleteChecklist(itemId, me);
    }

    @PostMapping("/tasks/{id}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    public CommentDto comment(@AuthenticationPrincipal User me, @PathVariable Long id, @Valid @RequestBody CommentRequest r) {
        return CommentDto.from(tasks.addComment(id, r.body(), me));
    }

    @DeleteMapping("/comments/{commentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteComment(@AuthenticationPrincipal User me, @PathVariable Long commentId) {
        tasks.deleteComment(commentId, me);
    }

    @PostMapping("/tasks/{id}/worklogs")
    @ResponseStatus(HttpStatus.CREATED)
    public WorkLogDto logWork(@AuthenticationPrincipal User me, @PathVariable Long id, @Valid @RequestBody WorkLogRequest r) {
        return WorkLogDto.from(tasks.logWork(id, r, me));
    }

    @PostMapping("/tasks/{id}/attachments")
    @ResponseStatus(HttpStatus.CREATED)
    public AttachmentDto attach(@AuthenticationPrincipal User me, @PathVariable Long id, @RequestParam MultipartFile file) {
        return AttachmentDto.from(tasks.attach(id, file, me));
    }

    @GetMapping("/attachments/{attachmentId}")
    public ResponseEntity<Resource> download(@AuthenticationPrincipal User me, @PathVariable Long attachmentId) {
        Attachment a = tasks.attachment(attachmentId, me);
        return ResponseEntity.ok()
                .contentType(a.getContentType() == null ? MediaType.APPLICATION_OCTET_STREAM : MediaType.parseMediaType(a.getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(a.getFileName(), StandardCharsets.UTF_8).build().toString())
                .body(tasks.file(a));
    }

    @DeleteMapping("/attachments/{attachmentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAttachment(@AuthenticationPrincipal User me, @PathVariable Long attachmentId) {
        tasks.deleteAttachment(attachmentId, me);
    }

    @GetMapping("/projects/{projectId}/activity")
    public List<ActivityDto> activity(@AuthenticationPrincipal User me, @PathVariable Long projectId) {
        return tasks.projectActivity(projectId, me).stream().map(ActivityDto::from).toList();
    }
}

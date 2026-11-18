package com.taskflow.service;

import com.taskflow.dto.Dtos.*;
import com.taskflow.model.*;
import com.taskflow.repo.*;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static com.taskflow.web.ApiExceptionHandler.*;

/** Tasks and everything hanging off them: sub-tasks, checklist, attachments, comments, work logs, audit trail. */
@Service
@RequiredArgsConstructor
@Transactional
public class TaskService {
    private final TaskRepository tasks;
    private final ChecklistItemRepository checklist;
    private final AttachmentRepository attachments;
    private final CommentRepository comments;
    private final WorkLogRepository workLogs;
    private final ActivityLogRepository activity;
    private final SprintRepository sprints;
    private final ProjectService projects;
    private final MailService mail;
    private final FileStorage storage;

    public record Filter(Long assigneeId, Task.Priority priority, String label, Long sprintId, Boolean backlog,
                         Task.Status status, String q) {}

    // ---- access helpers ----
    public Task get(Long id, User u) {
        Task t = tasks.findById(id).orElseThrow(() -> notFound("Task"));
        projects.view(t.getProject().getId(), u);
        return t;
    }

    private Task writable(Long id, User u) {
        Task t = tasks.findById(id).orElseThrow(() -> notFound("Task"));
        projects.write(t.getProject().getId(), u);
        return t;
    }

    // ---- tasks ----
    @Transactional(readOnly = true)
    public List<Task> search(Long projectId, Filter f, User u) {
        projects.view(projectId, u);
        Specification<Task> spec = (root, query, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            ps.add(cb.equal(root.get("project").get("id"), projectId));
            ps.add(cb.isNull(root.get("parent")));
            if (f.assigneeId() != null) ps.add(cb.equal(root.get("assignee").get("id"), f.assigneeId()));
            if (f.priority() != null) ps.add(cb.equal(root.get("priority"), f.priority()));
            if (f.status() != null) ps.add(cb.equal(root.get("status"), f.status()));
            if (f.label() != null && !f.label().isBlank())
                ps.add(cb.like(cb.lower(root.get("labels")), "%" + f.label().trim().toLowerCase() + "%"));
            if (f.q() != null && !f.q().isBlank())
                ps.add(cb.like(cb.lower(root.get("title")), "%" + f.q().trim().toLowerCase() + "%"));
            if (Boolean.TRUE.equals(f.backlog())) ps.add(cb.isNull(root.get("sprint")));
            else if (f.sprintId() != null) ps.add(cb.equal(root.get("sprint").get("id"), f.sprintId()));
            return cb.and(ps.toArray(Predicate[]::new));
        };
        return tasks.findAll(spec, Sort.by("id"));
    }

    public Task create(Long projectId, TaskRequest r, User u) {
        Project p = projects.write(projectId, u);
        Task t = new Task();
        t.setProject(p);
        t.setReporter(u);
        apply(t, r);
        tasks.save(t);
        log(t, u, "CREATED", t.getTitle());
        if (t.getAssignee() != null) notifyAssigned(t, u);
        return t;
    }

    public Task createSubtask(Long parentId, TaskRequest r, User u) {
        Task parent = writable(parentId, u);
        if (parent.getParent() != null) throw badRequest("Sub-tasks cannot have their own sub-tasks");
        Task t = new Task();
        t.setProject(parent.getProject());
        t.setParent(parent);
        t.setReporter(u);
        apply(t, r);
        tasks.save(t);
        log(t, u, "CREATED", "Sub-task of #" + parentId + ": " + t.getTitle());
        if (t.getAssignee() != null) notifyAssigned(t, u);
        return t;
    }

    public Task update(Long id, TaskRequest r, User u) {
        Task t = writable(id, u);
        User before = t.getAssignee();
        apply(t, r);
        log(t, u, "UPDATED", t.getTitle());
        if (t.getAssignee() != null && !t.getAssignee().equals(before)) {
            log(t, u, "ASSIGNED", t.getAssignee().getFullName());
            notifyAssigned(t, u);
        }
        return t;
    }

    public Task updateStatus(Long id, Task.Status status, User u) {
        Task t = writable(id, u);
        Task.Status old = t.getStatus();
        if (old == status) return t;
        t.setStatus(status);
        t.setCompletedAt(status == Task.Status.DONE ? LocalDateTime.now() : null);
        t.setUpdatedAt(LocalDateTime.now());
        log(t, u, "STATUS_CHANGED", old + " -> " + status);
        notifyStatus(t, old, u);
        return t;
    }

    public void delete(Long id, User u) {
        Task t = writable(id, u);
        if (!(ProjectService.isManager(u) || t.getReporter().equals(u)))
            throw forbidden("Only the reporter or a project manager can delete this task");
        log(t.getProject(), null, u, "DELETED", "#" + t.getId() + " " + t.getTitle());
        tasks.delete(t); // DB cascades children; audit rows keep task_id = null
    }

    private void apply(Task t, TaskRequest r) {
        Project p = t.getProject();
        t.setTitle(r.title().trim());
        t.setDescription(r.description());
        t.setType(r.type());
        t.setPriority(r.priority());
        t.setLabels(r.labels() == null || r.labels().isBlank() ? null : r.labels().trim());
        t.setEstimatedHours(r.estimatedHours());
        t.setAssignee(r.assigneeId() == null ? null : p.getMembers().stream()
                .filter(m -> m.getId().equals(r.assigneeId())).findFirst()
                .orElseThrow(() -> badRequest("Assignee must be a project member")));
        if (t.getParent() != null) {
            t.setSprint(t.getParent().getSprint()); // sub-tasks follow their parent
        } else if (r.sprintId() == null) {
            t.setSprint(null);
        } else {
            Sprint s = sprints.findById(r.sprintId())
                    .filter(x -> x.getProject().getId().equals(p.getId()))
                    .orElseThrow(() -> badRequest("Sprint does not belong to this project"));
            if (s.getStatus() == Sprint.Status.COMPLETED) throw badRequest("Cannot add tasks to a completed sprint");
            t.setSprint(s);
        }
        t.setUpdatedAt(LocalDateTime.now());
    }

    @Transactional(readOnly = true)
    public TaskDetail detail(Long id, User u) {
        Task t = get(id, u);
        return new TaskDetail(TaskDto.from(t),
                tasks.findByParentIdOrderById(id).stream().map(TaskDto::from).toList(),
                checklist.findByTaskIdOrderById(id).stream().map(ChecklistDto::from).toList(),
                attachments.findByTaskIdOrderById(id).stream().map(AttachmentDto::from).toList(),
                comments.findByTaskIdOrderByCreatedAt(id).stream().map(CommentDto::from).toList(),
                workLogs.findByTaskIdOrderByLogDateDesc(id).stream().map(WorkLogDto::from).toList(),
                workLogs.sumByTask(id),
                activity.findByTaskIdOrderByCreatedAtDesc(id).stream().map(ActivityDto::from).toList());
    }

    // ---- checklist ----
    public ChecklistItem addChecklist(Long taskId, String text, User u) {
        ChecklistItem c = new ChecklistItem();
        c.setTask(writable(taskId, u));
        c.setText(text.trim());
        return checklist.save(c);
    }

    public ChecklistItem toggleChecklist(Long itemId, User u) {
        ChecklistItem c = checklist.findById(itemId).orElseThrow(() -> notFound("Checklist item"));
        writable(c.getTask().getId(), u);
        c.setDone(!c.isDone());
        return c;
    }

    public void deleteChecklist(Long itemId, User u) {
        ChecklistItem c = checklist.findById(itemId).orElseThrow(() -> notFound("Checklist item"));
        writable(c.getTask().getId(), u);
        checklist.delete(c);
    }

    // ---- comments ----
    public Comment addComment(Long taskId, String body, User u) {
        Comment c = new Comment();
        c.setTask(writable(taskId, u));
        c.setAuthor(u);
        c.setBody(body.trim());
        return comments.save(c);
    }

    public void deleteComment(Long id, User u) {
        Comment c = comments.findById(id).orElseThrow(() -> notFound("Comment"));
        get(c.getTask().getId(), u);
        if (!(c.getAuthor().equals(u) || ProjectService.isAdmin(u))) throw forbidden("You can only delete your own comments");
        comments.delete(c);
    }

    // ---- time tracking ----
    public WorkLog logWork(Long taskId, WorkLogRequest r, User u) {
        Task t = writable(taskId, u);
        WorkLog w = new WorkLog();
        w.setTask(t);
        w.setUser(u);
        w.setHours(r.hours());
        w.setNote(r.note());
        w.setLogDate(r.logDate() == null ? LocalDate.now() : r.logDate());
        workLogs.save(w);
        log(t, u, "WORK_LOGGED", r.hours() + "h");
        return w;
    }

    // ---- attachments ----
    public Attachment attach(Long taskId, MultipartFile file, User u) {
        Task t = writable(taskId, u);
        Attachment a = new Attachment();
        a.setTask(t);
        a.setFileName(file.getOriginalFilename() == null ? "file" : file.getOriginalFilename());
        a.setContentType(file.getContentType());
        a.setSize(file.getSize());
        a.setUploadedBy(u);
        a.setStoredName(storage.save(file));
        attachments.save(a);
        log(t, u, "ATTACHED", a.getFileName());
        return a;
    }

    @Transactional(readOnly = true)
    public Attachment attachment(Long id, User u) {
        Attachment a = attachments.findById(id).orElseThrow(() -> notFound("Attachment"));
        get(a.getTask().getId(), u);
        return a;
    }

    public void deleteAttachment(Long id, User u) {
        Attachment a = attachments.findById(id).orElseThrow(() -> notFound("Attachment"));
        writable(a.getTask().getId(), u);
        if (!(a.getUploadedBy().equals(u) || ProjectService.isManager(u))) throw forbidden("Not allowed");
        attachments.delete(a);
        storage.delete(a.getStoredName());
    }

    public org.springframework.core.io.Resource file(Attachment a) {
        return storage.load(a.getStoredName());
    }

    // ---- audit ----
    @Transactional(readOnly = true)
    public List<ActivityLog> projectActivity(Long projectId, User u) {
        projects.view(projectId, u);
        return activity.findTop100ByProjectIdOrderByCreatedAtDesc(projectId);
    }

    private void log(Task t, User u, String action, String details) {
        log(t.getProject(), t, u, action, details);
    }

    private void log(Project p, Task t, User u, String action, String details) {
        ActivityLog a = new ActivityLog();
        a.setProject(p);
        a.setTask(t);
        a.setUser(u);
        a.setAction(action);
        a.setDetails(details);
        activity.save(a);
    }

    // ---- notifications ----
    private String taskLink(Task t) {
        return mail.link("/projects/" + t.getProject().getId() + "?task=" + t.getId());
    }

    private void notifyAssigned(Task t, User actor) {
        if (t.getAssignee().equals(actor)) return;
        mail.send(t.getAssignee().getEmail(), "[TaskFlow] Assigned: " + t.getTitle(),
                "Hi " + t.getAssignee().getFullName() + ",\n\n" + actor.getFullName() + " assigned you to \""
                        + t.getTitle() + "\" in " + t.getProject().getName() + ".\nPriority: " + t.getPriority()
                        + "\nStatus: " + t.getStatus() + "\n\n" + taskLink(t));
    }

    private void notifyStatus(Task t, Task.Status old, User actor) {
        Set<User> to = new LinkedHashSet<>();
        if (t.getAssignee() != null) to.add(t.getAssignee());
        to.add(t.getReporter());
        to.stream().filter(Objects::nonNull).filter(r -> !r.equals(actor)).forEach(r ->
                mail.send(r.getEmail(), "[TaskFlow] " + t.getTitle() + ": " + old + " -> " + t.getStatus(),
                        "Hi " + r.getFullName() + ",\n\n" + actor.getFullName() + " moved \"" + t.getTitle()
                                + "\" from " + old + " to " + t.getStatus() + ".\n\n" + taskLink(t)));
    }
}

package com.taskflow.service;

import com.taskflow.dto.Dtos.*;
import com.taskflow.model.*;
import com.taskflow.repo.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

import static com.taskflow.web.ApiExceptionHandler.*;

/**
 * Projects, membership, sprints and the access rules everything else relies on:
 * view  = admin, owner or member;
 * write = view + project not archived;
 * manage = view + role ADMIN or PROJECT_MANAGER.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class ProjectService {
    private final ProjectRepository projects;
    private final UserRepository users;
    private final SprintRepository sprints;
    private final TaskRepository tasks;

    public static boolean isAdmin(User u) {
        return u.getRole() == User.Role.ADMIN;
    }

    public static boolean isManager(User u) {
        return u.getRole() == User.Role.ADMIN || u.getRole() == User.Role.PROJECT_MANAGER;
    }

    public Project view(Long id, User u) {
        Project p = projects.findById(id).orElseThrow(() -> notFound("Project"));
        if (!(isAdmin(u) || p.getOwner().equals(u) || p.getMembers().contains(u)))
            throw forbidden("You are not a member of this project");
        return p;
    }

    public Project write(Long id, User u) {
        Project p = view(id, u);
        if (p.isArchived()) throw badRequest("Project is archived");
        return p;
    }

    public Project manage(Long id, User u) {
        Project p = view(id, u);
        if (!isManager(u)) throw forbidden("Only admins and project managers can do this");
        return p;
    }

    @Transactional(readOnly = true)
    public List<Project> list(User u, boolean archived) {
        List<Project> all = isAdmin(u) ? projects.findAll() : projects.findVisible(u);
        return all.stream().filter(p -> p.isArchived() == archived)
                .sorted(Comparator.comparing(Project::getId).reversed()).toList();
    }

    public Project create(ProjectRequest r, User u) {
        Project p = new Project();
        p.setName(r.name().trim());
        p.setDescription(r.description());
        p.setOwner(u);
        p.getMembers().add(u);
        return projects.save(p);
    }

    public Project update(Long id, ProjectRequest r, User u) {
        Project p = manage(id, u);
        p.setName(r.name().trim());
        p.setDescription(r.description());
        return p;
    }

    public Project setArchived(Long id, boolean archived, User u) {
        Project p = manage(id, u);
        p.setArchived(archived);
        return p;
    }

    public Project addMember(Long id, String email, User u) {
        Project p = manage(id, u);
        User m = users.findByEmailIgnoreCase(email.trim()).orElseThrow(() -> notFound("User with that email"));
        if (!m.isActive()) throw badRequest("User is deactivated");
        p.getMembers().add(m);
        return p;
    }

    public Project removeMember(Long id, Long userId, User u) {
        Project p = manage(id, u);
        if (p.getOwner().getId().equals(userId)) throw badRequest("The project owner cannot be removed");
        p.getMembers().removeIf(m -> m.getId().equals(userId));
        return p;
    }

    // ---- sprints ----
    @Transactional(readOnly = true)
    public List<Sprint> sprints(Long projectId, User u) {
        view(projectId, u);
        return sprints.findByProjectIdOrderByIdDesc(projectId);
    }

    public Sprint sprint(Long id) {
        return sprints.findById(id).orElseThrow(() -> notFound("Sprint"));
    }

    public Sprint createSprint(Long projectId, SprintRequest r, User u) {
        Project p = manage(projectId, u);
        if (p.isArchived()) throw badRequest("Project is archived");
        Sprint s = new Sprint();
        s.setProject(p);
        apply(s, r);
        return sprints.save(s);
    }

    public Sprint updateSprint(Long id, SprintRequest r, User u) {
        Sprint s = sprint(id);
        manage(s.getProject().getId(), u);
        apply(s, r);
        return s;
    }

    private static void apply(Sprint s, SprintRequest r) {
        if (r.startDate() != null && r.endDate() != null && r.endDate().isBefore(r.startDate()))
            throw badRequest("End date must be on or after start date");
        s.setName(r.name().trim());
        s.setGoal(r.goal());
        s.setStartDate(r.startDate());
        s.setEndDate(r.endDate());
    }

    public Sprint startSprint(Long id, User u) {
        Sprint s = sprint(id);
        manage(s.getProject().getId(), u);
        if (s.getStatus() != Sprint.Status.PLANNED) throw badRequest("Only planned sprints can be started");
        if (sprints.existsByProjectIdAndStatus(s.getProject().getId(), Sprint.Status.ACTIVE))
            throw badRequest("Another sprint is already active");
        if (s.getStartDate() == null) s.setStartDate(LocalDate.now());
        if (s.getEndDate() == null) s.setEndDate(s.getStartDate().plusDays(13));
        s.setStatus(Sprint.Status.ACTIVE);
        return s;
    }

    /** Completing a sprint returns unfinished work to the backlog. */
    public Sprint completeSprint(Long id, User u) {
        Sprint s = sprint(id);
        manage(s.getProject().getId(), u);
        if (s.getStatus() != Sprint.Status.ACTIVE) throw badRequest("Only the active sprint can be completed");
        s.setStatus(Sprint.Status.COMPLETED);
        tasks.findBySprintId(id).stream().filter(t -> t.getStatus() != Task.Status.DONE).forEach(t -> t.setSprint(null));
        return s;
    }
}

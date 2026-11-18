package com.taskflow.web;

import com.taskflow.dto.Dtos.*;
import com.taskflow.model.User;
import com.taskflow.service.ProjectService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Projects & Sprints")
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ProjectController {
    private final ProjectService projects;

    @GetMapping("/projects")
    public List<ProjectDto> list(@AuthenticationPrincipal User me, @RequestParam(defaultValue = "false") boolean archived) {
        return projects.list(me, archived).stream().map(ProjectDto::from).toList();
    }

    @PostMapping("/projects")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN','PROJECT_MANAGER')")
    public ProjectDto create(@AuthenticationPrincipal User me, @Valid @RequestBody ProjectRequest r) {
        return ProjectDto.from(projects.create(r, me));
    }

    @GetMapping("/projects/{id}")
    public ProjectDto get(@AuthenticationPrincipal User me, @PathVariable Long id) {
        return ProjectDto.from(projects.view(id, me));
    }

    @PutMapping("/projects/{id}")
    public ProjectDto update(@AuthenticationPrincipal User me, @PathVariable Long id, @Valid @RequestBody ProjectRequest r) {
        return ProjectDto.from(projects.update(id, r, me));
    }

    @PostMapping("/projects/{id}/archive")
    public ProjectDto archive(@AuthenticationPrincipal User me, @PathVariable Long id) {
        return ProjectDto.from(projects.setArchived(id, true, me));
    }

    @PostMapping("/projects/{id}/unarchive")
    public ProjectDto unarchive(@AuthenticationPrincipal User me, @PathVariable Long id) {
        return ProjectDto.from(projects.setArchived(id, false, me));
    }

    @PostMapping("/projects/{id}/members")
    public ProjectDto addMember(@AuthenticationPrincipal User me, @PathVariable Long id, @Valid @RequestBody MemberRequest r) {
        return ProjectDto.from(projects.addMember(id, r.email(), me));
    }

    @DeleteMapping("/projects/{id}/members/{userId}")
    public ProjectDto removeMember(@AuthenticationPrincipal User me, @PathVariable Long id, @PathVariable Long userId) {
        return ProjectDto.from(projects.removeMember(id, userId, me));
    }

    @GetMapping("/projects/{id}/sprints")
    public List<SprintDto> sprints(@AuthenticationPrincipal User me, @PathVariable Long id) {
        return projects.sprints(id, me).stream().map(SprintDto::from).toList();
    }

    @PostMapping("/projects/{id}/sprints")
    @ResponseStatus(HttpStatus.CREATED)
    public SprintDto createSprint(@AuthenticationPrincipal User me, @PathVariable Long id, @Valid @RequestBody SprintRequest r) {
        return SprintDto.from(projects.createSprint(id, r, me));
    }

    @PutMapping("/sprints/{id}")
    public SprintDto updateSprint(@AuthenticationPrincipal User me, @PathVariable Long id, @Valid @RequestBody SprintRequest r) {
        return SprintDto.from(projects.updateSprint(id, r, me));
    }

    @PostMapping("/sprints/{id}/start")
    public SprintDto start(@AuthenticationPrincipal User me, @PathVariable Long id) {
        return SprintDto.from(projects.startSprint(id, me));
    }

    @PostMapping("/sprints/{id}/complete")
    public SprintDto complete(@AuthenticationPrincipal User me, @PathVariable Long id) {
        return SprintDto.from(projects.completeSprint(id, me));
    }
}

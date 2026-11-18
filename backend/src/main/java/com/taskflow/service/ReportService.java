package com.taskflow.service;

import com.lowagie.text.Document;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.taskflow.dto.Dtos.*;
import com.taskflow.model.*;
import com.taskflow.repo.SprintRepository;
import com.taskflow.repo.TaskRepository;
import com.taskflow.repo.WorkLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportService {
    private final ProjectService projects;
    private final TaskRepository tasks;
    private final SprintRepository sprints;
    private final WorkLogRepository workLogs;

    private static final String[] HEADERS = {"ID", "Title", "Type", "Priority", "Status", "Assignee", "Sprint",
            "Labels", "Estimated (h)", "Created", "Completed"};

    public DashboardDto dashboard(Long projectId, User u) {
        projects.view(projectId, u);
        List<Task> all = tasks.findByProjectIdAndParentIsNullOrderById(projectId);
        Map<Task.Status, Long> byStatus = new EnumMap<>(Task.Status.class);
        Map<Task.Priority, Long> byPriority = new EnumMap<>(Task.Priority.class);
        for (Task.Status s : Task.Status.values()) byStatus.put(s, 0L);
        for (Task.Priority p : Task.Priority.values()) byPriority.put(p, 0L);
        all.forEach(t -> {
            byStatus.merge(t.getStatus(), 1L, Long::sum);
            byPriority.merge(t.getPriority(), 1L, Long::sum);
        });
        long done = byStatus.get(Task.Status.DONE);
        double estimated = all.stream().map(Task::getEstimatedHours).filter(Objects::nonNull).mapToDouble(Double::doubleValue).sum();
        return new DashboardDto(all.size(), done, all.isEmpty() ? 0 : Math.round(done * 1000.0 / all.size()) / 10.0,
                byStatus, byPriority, estimated, workLogs.sumByProject(projectId), burndown(projectId));
    }

    /** Remaining open tasks per day of the active (or most recent completed) sprint vs. an ideal straight line. */
    Burndown burndown(Long projectId) {
        Sprint s = sprints.findFirstByProjectIdAndStatusOrderByIdDesc(projectId, Sprint.Status.ACTIVE)
                .or(() -> sprints.findFirstByProjectIdAndStatusOrderByIdDesc(projectId, Sprint.Status.COMPLETED))
                .orElse(null);
        if (s == null || s.getStartDate() == null || s.getEndDate() == null) return null;
        List<Task> sprintTasks = tasks.findBySprintId(s.getId()).stream().filter(t -> t.getParent() == null).toList();
        int total = sprintTasks.size();
        long days = ChronoUnit.DAYS.between(s.getStartDate(), s.getEndDate()) + 1;
        LocalDate today = LocalDate.now();
        List<BurndownPoint> points = new ArrayList<>();
        for (int i = 0; i < days; i++) {
            LocalDate day = s.getStartDate().plusDays(i);
            Long remaining = day.isAfter(today) ? null : sprintTasks.stream()
                    .filter(t -> t.getCompletedAt() == null || t.getCompletedAt().toLocalDate().isAfter(day)).count();
            double ideal = days == 1 ? 0 : total * (1 - (double) i / (days - 1));
            points.add(new BurndownPoint(day, remaining, Math.round(ideal * 10) / 10.0));
        }
        return new Burndown(s.getName(), total, points);
    }

    private static List<String> row(Task t) {
        return List.of(String.valueOf(t.getId()), t.getTitle(), t.getType().name(), t.getPriority().name(),
                t.getStatus().name(), t.getAssignee() == null ? "" : t.getAssignee().getFullName(),
                t.getSprint() == null ? "Backlog" : t.getSprint().getName(), Objects.toString(t.getLabels(), ""),
                t.getEstimatedHours() == null ? "" : String.valueOf(t.getEstimatedHours()),
                t.getCreatedAt().toLocalDate().toString(),
                t.getCompletedAt() == null ? "" : t.getCompletedAt().toLocalDate().toString());
    }

    public String projectName(Long projectId, User u) {
        return projects.view(projectId, u).getName();
    }

    public byte[] csv(Long projectId, User u) {
        projects.view(projectId, u);
        StringBuilder sb = new StringBuilder(String.join(",", HEADERS)).append("\r\n");
        for (Task t : tasks.findByProjectIdAndParentIsNullOrderById(projectId))
            sb.append(row(t).stream().map(ReportService::csvCell).collect(Collectors.joining(","))).append("\r\n");
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    static String csvCell(String v) {
        if (!v.isEmpty() && "=+-@".indexOf(v.charAt(0)) >= 0) v = "'" + v; // block spreadsheet formula injection
        return "\"" + v.replace("\"", "\"\"") + "\"";
    }

    public byte[] pdf(Long projectId, User u) {
        Project p = projects.view(projectId, u);
        DashboardDto d = dashboard(projectId, u);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4.rotate(), 30, 30, 30, 30);
        PdfWriter.getInstance(doc, out);
        doc.open();
        doc.add(new Paragraph("TaskFlow report - " + p.getName(), FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16)));
        doc.add(new Paragraph(String.format("Generated %s   |   Tasks: %d   |   Done: %d (%.1f%%)   |   Estimated: %.1fh   |   Logged: %.1fh",
                LocalDate.now(), d.totalTasks(), d.doneTasks(), d.completionPercent(), d.estimatedHours(), d.loggedHours()),
                FontFactory.getFont(FontFactory.HELVETICA, 10)));
        doc.add(new Paragraph(" "));
        PdfPTable table = new PdfPTable(HEADERS.length);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{4, 22, 6, 7, 9, 12, 10, 10, 6, 8, 8});
        Font head = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8);
        Font cell = FontFactory.getFont(FontFactory.HELVETICA, 8);
        for (String h : HEADERS) {
            PdfPCell c = new PdfPCell(new Phrase(h, head));
            c.setBackgroundColor(new Color(225, 228, 240));
            table.addCell(c);
        }
        for (Task t : tasks.findByProjectIdAndParentIsNullOrderById(projectId))
            row(t).forEach(v -> table.addCell(new Phrase(v, cell)));
        doc.add(table);
        doc.close();
        return out.toByteArray();
    }
}

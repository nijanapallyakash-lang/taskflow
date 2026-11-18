package com.taskflow.web;

import com.taskflow.dto.Dtos.DashboardDto;
import com.taskflow.model.User;
import com.taskflow.service.ReportService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;

import static com.taskflow.web.ApiExceptionHandler.badRequest;

@Tag(name = "Dashboard & Reports")
@RestController
@RequestMapping("/api/projects/{id}")
@RequiredArgsConstructor
public class ReportController {
    private final ReportService reports;

    @GetMapping("/dashboard")
    public DashboardDto dashboard(@AuthenticationPrincipal User me, @PathVariable Long id) {
        return reports.dashboard(id, me);
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> export(@AuthenticationPrincipal User me, @PathVariable Long id,
                                         @RequestParam(defaultValue = "csv") String format) {
        boolean pdf = switch (format.toLowerCase()) {
            case "pdf" -> true;
            case "csv" -> false;
            default -> throw badRequest("format must be csv or pdf");
        };
        String name = reports.projectName(id, me).replaceAll("[^A-Za-z0-9_-]+", "_") + "-report." + (pdf ? "pdf" : "csv");
        return ResponseEntity.ok()
                .contentType(pdf ? MediaType.APPLICATION_PDF : new MediaType("text", "csv", StandardCharsets.UTF_8))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(name).build().toString())
                .body(pdf ? reports.pdf(id, me) : reports.csv(id, me));
    }
}

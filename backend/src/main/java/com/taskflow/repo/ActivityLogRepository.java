package com.taskflow.repo;

import com.taskflow.model.ActivityLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ActivityLogRepository extends JpaRepository<ActivityLog, Long> {
    List<ActivityLog> findByTaskIdOrderByCreatedAtDesc(Long taskId);
    List<ActivityLog> findTop100ByProjectIdOrderByCreatedAtDesc(Long projectId);
}

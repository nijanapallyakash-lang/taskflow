package com.taskflow.repo;

import com.taskflow.model.WorkLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface WorkLogRepository extends JpaRepository<WorkLog, Long> {
    List<WorkLog> findByTaskIdOrderByLogDateDesc(Long taskId);

    @Query("select coalesce(sum(w.hours), 0) from WorkLog w where w.task.id = :taskId")
    double sumByTask(Long taskId);

    @Query("select coalesce(sum(w.hours), 0) from WorkLog w where w.task.project.id = :projectId")
    double sumByProject(Long projectId);
}

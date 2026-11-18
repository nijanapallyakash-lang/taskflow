package com.taskflow.repo;

import com.taskflow.model.ChecklistItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChecklistItemRepository extends JpaRepository<ChecklistItem, Long> {
    List<ChecklistItem> findByTaskIdOrderById(Long taskId);
}

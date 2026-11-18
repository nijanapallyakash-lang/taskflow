package com.taskflow.repo;

import com.taskflow.model.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface TaskRepository extends JpaRepository<Task, Long>, JpaSpecificationExecutor<Task> {
    List<Task> findByParentIdOrderById(Long parentId);
    List<Task> findBySprintId(Long sprintId);
    List<Task> findByProjectIdAndParentIsNullOrderById(Long projectId);
}

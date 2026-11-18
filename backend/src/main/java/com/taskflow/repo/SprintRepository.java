package com.taskflow.repo;

import com.taskflow.model.Sprint;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SprintRepository extends JpaRepository<Sprint, Long> {
    List<Sprint> findByProjectIdOrderByIdDesc(Long projectId);
    boolean existsByProjectIdAndStatus(Long projectId, Sprint.Status status);
    Optional<Sprint> findFirstByProjectIdAndStatusOrderByIdDesc(Long projectId, Sprint.Status status);
}

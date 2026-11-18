package com.taskflow.repo;

import com.taskflow.model.Project;
import com.taskflow.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ProjectRepository extends JpaRepository<Project, Long> {
    @Query("select distinct p from Project p left join p.members m where p.owner = :u or m = :u")
    List<Project> findVisible(User u);
}

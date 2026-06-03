package com.formedit.repository;

import com.formedit.entity.WorkflowTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WorkflowTaskRepository extends JpaRepository<WorkflowTask, Long> {

    List<WorkflowTask> findByInstanceIdOrderByIdAsc(Long instanceId);

    List<WorkflowTask> findByInstanceIdAndStatusOrderByIdAsc(Long instanceId, String status);

    List<WorkflowTask> findByInstanceIdAndNodeIdOrderByIdAsc(Long instanceId, String nodeId);

    List<WorkflowTask> findByInstanceIdAndNodeIdAndStatusOrderByIdAsc(Long instanceId, String nodeId, String status);
}

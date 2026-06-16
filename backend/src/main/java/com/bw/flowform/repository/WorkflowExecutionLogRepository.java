package com.bw.flowform.repository;

import com.bw.flowform.entity.WorkflowExecutionLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WorkflowExecutionLogRepository extends JpaRepository<WorkflowExecutionLog, Long> {

    List<WorkflowExecutionLog> findByInstanceIdOrderByIdAsc(Long instanceId);

    List<WorkflowExecutionLog> findByInstanceIdOrderByIdDesc(Long instanceId);

    List<WorkflowExecutionLog> findByInstanceIdAndNodeIdOrderByIdAsc(Long instanceId, String nodeId);
}

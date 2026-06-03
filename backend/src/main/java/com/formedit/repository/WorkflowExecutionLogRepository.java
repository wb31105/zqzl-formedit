package com.formedit.repository;

import com.formedit.entity.WorkflowExecutionLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WorkflowExecutionLogRepository extends JpaRepository<WorkflowExecutionLog, Long> {

    List<WorkflowExecutionLog> findByInstanceIdOrderByIdAsc(Long instanceId);

    List<WorkflowExecutionLog> findByInstanceIdOrderByIdDesc(Long instanceId);
}

package com.formedit.repository;

import com.formedit.entity.WorkflowInstance;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WorkflowInstanceRepository extends JpaRepository<WorkflowInstance, Long> {

    Page<WorkflowInstance> findAllByOrderByIdDesc(Pageable pageable);

    List<WorkflowInstance> findByDefinitionIdOrderByIdDesc(Long definitionId);
}

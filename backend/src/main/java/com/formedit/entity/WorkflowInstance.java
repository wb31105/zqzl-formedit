package com.formedit.entity;

import lombok.Data;

import javax.persistence.*;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "workflow_instances")
public class WorkflowInstance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "definition_id", nullable = false)
    private Long definitionId;

    @Column(name = "definition_name", nullable = false)
    private String definitionName;

    @Column(nullable = false)
    private String status;

    @Column(name = "current_node_id")
    private String currentNodeId;

    @Column(name = "current_node_name")
    private String currentNodeName;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "ended_at")
    private LocalDateTime endedAt;

    @PrePersist
    protected void onCreate() {
        startedAt = LocalDateTime.now();
    }
}

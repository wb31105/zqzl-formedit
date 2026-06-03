package com.formedit.entity;

import lombok.Data;

import javax.persistence.*;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "workflow_execution_logs")
public class WorkflowExecutionLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "instance_id", nullable = false)
    private Long instanceId;

    @Column(name = "node_id", nullable = false)
    private String nodeId;

    @Column(name = "node_name", nullable = false)
    private String nodeName;

    @Column(name = "node_type", nullable = false)
    private String nodeType;

    @Column(nullable = false)
    private String action;

    @Column(length = 1000)
    private String comment;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}

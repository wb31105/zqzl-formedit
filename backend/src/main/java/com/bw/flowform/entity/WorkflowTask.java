package com.bw.flowform.entity;

import lombok.Data;

import javax.persistence.*;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "workflow_tasks")
public class WorkflowTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "instance_id", nullable = false)
    private Long instanceId;

    @Column(name = "node_id", nullable = false)
    private String nodeId;

    @Column(name = "node_name", nullable = false)
    private String nodeName;

    @Column(nullable = false)
    private String status;

    @Column(name = "assignee")
    private String assignee;

    @Column(length = 1000)
    private String comment;

    @Column(name = "action")
    private String action;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}

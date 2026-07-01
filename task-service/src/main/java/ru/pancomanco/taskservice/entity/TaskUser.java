package ru.pancomanco.taskservice.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "task_users")
@Getter
@Setter
@NoArgsConstructor
public class TaskUser {

    @Id
    @Column(name = "user_id", updatable = false)
    private Long userId;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private String name;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public TaskUser(Long userId, String email, String name) {
        this.userId = userId;
        this.email = email;
        this.name = name;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }
}

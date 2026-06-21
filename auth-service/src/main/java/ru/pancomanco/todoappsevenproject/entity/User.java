package ru.pancomanco.todoappsevenproject.entity;

import jakarta.persistence.*;
import lombok.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import ru.pancomanco.todoappsevenproject.util.EmailUtil;

import java.time.Instant;

@Entity
@Table(
        name = "users",
        indexes = {
                @Index(name = "idx_users_enabled_created_at", columnList = "enabled, created_at")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = "password")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String email;

    @JsonIgnore
    @Column(nullable = true)
    private String password;

    @Column(length = 100)
    private String name;

    @Column(length = 500)
    private String avatarUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role = Role.USER;

    @Column(nullable = false)
    private Boolean enabled=false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

//    public User (String email, String password) {
//        this.email = email;
//        this.password = password;
//    }

    public User(String email, String password) {
        this.email = email;
        this.password = password;
        this.enabled = false;
        this.role = Role.USER;
    }
    public static User socialUser(
            String email,
            String name,
            String avatarUrl
    ) {
        User user = new User();
        user.email = email.toLowerCase();
        user.password = null;
        user.name = name;
        user.avatarUrl = avatarUrl;
        user.enabled = true;
        user.role = Role.USER;
        return user;
    }
    @PrePersist
    void prePersist() {
        email = EmailUtil.normalize(email);
        Instant now = Instant.now();

        if (createdAt == null) {
            createdAt = now;
        }

        if (updatedAt == null) {
            updatedAt = now;
        }

        if (enabled == null) {
            enabled = false;
        }
        if (role == null) {
            role = Role.USER;
        }
    }

    @PreUpdate
    void preUpdate() {
        email = EmailUtil.normalize(email);
        updatedAt = Instant.now();
    }
}

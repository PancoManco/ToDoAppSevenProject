package ru.pancomanco.todoappsevenproject.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "users")
@Getter
@Setter
@RequiredArgsConstructor
@AllArgsConstructor
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(unique = true, nullable = true)
    private String password;

    private String name;

    private String avatarUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role = Role.USER;

    @Column(nullable = false)
    private boolean enabled=true;

    public User (String email, String password) {
        this.email = email;
        this.password = password;
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

    public static User socialUserWithoutEmail(
            String name,
            String avatarUrl
    ) {
        User user = new User();
        user.email = null;
        user.password = null;
        user.name = name;
        user.avatarUrl = avatarUrl;
        user.enabled = true;
        user.role = Role.USER;
        return user;
    }
}

package ru.pancomanco.todoappsevenproject.entity;

import jakarta.persistence.*;
import lombok.*;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name = "users")
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
    private Boolean enabled=true;

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

}

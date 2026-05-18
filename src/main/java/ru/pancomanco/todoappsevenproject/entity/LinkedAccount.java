package ru.pancomanco.todoappsevenproject.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.security.AuthProvider;

@Entity
@Table(name = "linked_accounts",
        uniqueConstraints = {
        @UniqueConstraint(
                name = "uk_provider_provider_user_id",
                columnNames = {"provider", "providerUserId"}
        )
}
)
@Getter
@AllArgsConstructor
public class LinkedAccount {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AuthProviderEnum provider;

    @Column(nullable = false)
    private String providerUserId;

    @Column
    private String providerEmail;


    public LinkedAccount(
            User user,
            AuthProviderEnum provider,
            String providerUserId,
            String providerEmail
    ) {
        this.user = user;
        this.provider = provider;
        this.providerUserId = providerUserId;
        this.providerEmail = providerEmail;
    }

}

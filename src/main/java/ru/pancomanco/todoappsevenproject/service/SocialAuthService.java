package ru.pancomanco.todoappsevenproject.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.pancomanco.todoappsevenproject.entity.AuthProviderEnum;
import ru.pancomanco.todoappsevenproject.entity.LinkedAccount;
import ru.pancomanco.todoappsevenproject.entity.SocialProfile;
import ru.pancomanco.todoappsevenproject.entity.User;
import ru.pancomanco.todoappsevenproject.repository.AuthRepository;
import ru.pancomanco.todoappsevenproject.repository.LinkedAccountRepository;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class SocialAuthService {
    private final AuthRepository userRepository;
    private final LinkedAccountRepository linkedAccountRepository;


    @Transactional
    public User findOrCreateUser(
            String registrationId,
            Map<String, Object> attributes
    ) {
        AuthProviderEnum provider = parseProvider(registrationId);

        SocialProfile profile = extractProfile(provider, attributes);

        return linkedAccountRepository
                .findByProviderAndProviderUserId(provider, profile.providerUserId())
                .map(LinkedAccount::getUser)
                .orElseGet(() -> createUserAndLinkedAccount(provider, profile));
    }

    private User createUserAndLinkedAccount(
            AuthProviderEnum provider,
            SocialProfile profile
    ) {
        User user;

        if (profile.email() != null && !profile.email().isBlank()) {
            user = userRepository.findByEmailIgnoreCase(profile.email())
                    .orElseGet(() -> userRepository.save(
                            User.socialUser(profile.email(), profile.name(), profile.avatarUrl())
                    ));
        } else {
            // GitHub email может быть null.
            // В таком случае создаём пользователя без email
            // или отправляем на экран "добавь email".
            user = userRepository.save(
                    User.socialUserWithoutEmail(profile.name(), profile.avatarUrl())
            );
        }

        LinkedAccount linkedAccount = new LinkedAccount(
                user,
                provider,
                profile.providerUserId(),
                profile.email()
        );
        linkedAccountRepository.save(linkedAccount);
        return user;
    }

    private AuthProviderEnum parseProvider(String registrationId) {
        return switch (registrationId.toLowerCase()) {
            case "google" -> AuthProviderEnum.GOOGLE;
            case "github" -> AuthProviderEnum.GITHUB;
            default -> throw new IllegalArgumentException("Unsupported provider: " + registrationId);
        };
    }

    private SocialProfile extractProfile(
            AuthProviderEnum provider,
            Map<String, Object> attributes
    ) {
        return switch (provider) {
            case GOOGLE -> extractGoogleProfile(attributes);
            case GITHUB -> extractGithubProfile(attributes);
        };
    }

    private SocialProfile extractGoogleProfile(Map<String, Object> attributes) {
        String providerUserId = (String) attributes.get("sub");
        String email = (String) attributes.get("email");
        String name = (String) attributes.get("name");
        String picture = (String) attributes.get("picture");

        return new SocialProfile(
                providerUserId,
                email,
                name,
                picture
        );
    }

    private SocialProfile extractGithubProfile(Map<String, Object> attributes) {
        String providerUserId = String.valueOf(attributes.get("id"));
        String email = (String) attributes.get("email");
        String name = (String) attributes.get("name");
        String login = (String) attributes.get("login");
        String avatarUrl = (String) attributes.get("avatar_url");

        if (name == null || name.isBlank()) {
            name = login;
        }

        return new SocialProfile(
                providerUserId,
                email,
                name,
                avatarUrl
        );
    }
}
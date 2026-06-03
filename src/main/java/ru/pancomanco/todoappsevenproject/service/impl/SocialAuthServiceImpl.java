package ru.pancomanco.todoappsevenproject.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.pancomanco.todoappsevenproject.entity.AuthProviderEnum;
import ru.pancomanco.todoappsevenproject.entity.LinkedAccount;
import ru.pancomanco.todoappsevenproject.entity.SocialProfile;
import ru.pancomanco.todoappsevenproject.entity.User;
import ru.pancomanco.todoappsevenproject.exception.ErrorCode;
import ru.pancomanco.todoappsevenproject.exception.SocialAuthException;
import ru.pancomanco.todoappsevenproject.repository.AuthRepository;
import ru.pancomanco.todoappsevenproject.repository.LinkedAccountRepository;
import ru.pancomanco.todoappsevenproject.service.SocialAuthService;

import java.util.Map;

@RequiredArgsConstructor
@Service
public class SocialAuthServiceImpl implements SocialAuthService {

    private final AuthRepository authRepository;
    private final LinkedAccountRepository linkedAccountRepository;


    @Transactional
    @Override
    public User findOrCreateUser(
            String registrationId,
            Map<String, Object> attributes
    ) {
        AuthProviderEnum provider = parseProvider(registrationId);

        SocialProfile profile = extractProfile(provider, attributes);

        return linkedAccountRepository
                .findByProviderAndProviderUserIdWithUser(provider, profile.providerUserId())
                .map(LinkedAccount::getUser)
                .orElseGet(() -> createUserAndLinkedAccount(provider, profile));
    }

    private User createUserAndLinkedAccount(
            AuthProviderEnum provider,
            SocialProfile profile
    ) {
        String normalizedEmail = profile.email().trim().toLowerCase();
        User user = authRepository.findByEmailIgnoreCase(normalizedEmail)
                .orElseGet(() -> authRepository.save(
                        User.socialUser(
                                normalizedEmail,
                                profile.name(),
                                profile.avatarUrl()
                        )
                ));

        if (!Boolean.TRUE.equals(user.getEnabled())) {
            user.setEnabled(true);
        }

        LinkedAccount linkedAccount = new LinkedAccount(
                user,
                provider,
                profile.providerUserId(),
                normalizedEmail
        );
        linkedAccountRepository.save(linkedAccount);
        return user;
    }

    private AuthProviderEnum parseProvider(String registrationId) {
        if (registrationId == null || registrationId.isBlank()) {
            throw new SocialAuthException(
                    ErrorCode.AUTH_SOCIAL_PROVIDER_UNSUPPORTED
            );
        }
        try {
            AuthProviderEnum provider =
                    AuthProviderEnum.valueOf(registrationId.toUpperCase());

            if (provider != AuthProviderEnum.GOOGLE) {
                throw new SocialAuthException(
                        ErrorCode.AUTH_SOCIAL_PROVIDER_UNSUPPORTED
                );
            }
            return provider;
        } catch (IllegalArgumentException ex) {
            throw new SocialAuthException(
                    ErrorCode.AUTH_SOCIAL_PROVIDER_UNSUPPORTED,
                    ex
            );
        }
//        return switch (registrationId.toLowerCase()) {
//            case "google" -> AuthProviderEnum.GOOGLE;
//            default -> throw new IllegalArgumentException("Unsupported provider: " + registrationId);
//        };
    }

    private SocialProfile extractProfile(
            AuthProviderEnum provider,
            Map<String, Object> attributes
    ) {
        return switch (provider) {
            case GOOGLE -> extractGoogleProfile(attributes);
        };
    }

    private SocialProfile extractGoogleProfile(Map<String, Object> attributes) {
        String providerUserId = (String) attributes.get("sub");
        String email = (String) attributes.get("email");
        String name = (String) attributes.get("name");
        String picture = (String) attributes.get("picture");

        Object emailVerifiedValue = attributes.get("email_verified");
        boolean emailVerified = Boolean.TRUE.equals(emailVerifiedValue);

        if (providerUserId == null || providerUserId.isBlank()) {
            throw new SocialAuthException(
                    ErrorCode.AUTH_SOCIAL_PROFILE_INVALID
            );
        }

        if (email == null || email.isBlank()) {
            throw new SocialAuthException(
                    ErrorCode.AUTH_SOCIAL_EMAIL_MISSING
            );
        }

        if (!emailVerified) {
            throw new SocialAuthException(
                    ErrorCode.AUTH_SOCIAL_EMAIL_NOT_VERIFIED
            );
        }

        String normalizedEmail = email.trim().toLowerCase();

        String displayName = name != null && !name.isBlank()
                ? name.trim()
                : normalizedEmail;

        return new SocialProfile(
                providerUserId.trim(),
                normalizedEmail,
                displayName,
                picture
        );
    }
}

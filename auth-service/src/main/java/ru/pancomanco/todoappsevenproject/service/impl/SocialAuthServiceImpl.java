package ru.pancomanco.todoappsevenproject.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
import ru.pancomanco.todoappsevenproject.util.EmailUtil;

import java.util.Map;
import java.util.Optional;

@RequiredArgsConstructor
@Service
@Slf4j
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
        String normalizedEmail = EmailUtil.normalize(profile.email());
        Optional<User> existingUserOpt = authRepository.findByEmail(normalizedEmail);
        if (existingUserOpt.isPresent()) {
            User existingUser = existingUserOpt.get();
            if (!Boolean.TRUE.equals(existingUser.getEnabled())) {
                log.warn("SECURITY ALERT: OAuth login attempt for unverified local account. Email: {}, Provider: {}", normalizedEmail, provider);
                throw new SocialAuthException(ErrorCode.AUTH_SOCIAL_UNVERIFIED_EMAIL_CONFLICT);
            }
            boolean alreadyLinked = linkedAccountRepository.existsByUserAndProvider(existingUser, provider);
            if (!alreadyLinked) {
                LinkedAccount linkedAccount = new LinkedAccount(
                        existingUser, provider, profile.providerUserId(), normalizedEmail
                );
                linkedAccountRepository.save(linkedAccount);
            }
            return existingUser;
        }
        User newUser = User.socialUser(normalizedEmail, profile.name(), profile.avatarUrl());
        authRepository.save(newUser);
        LinkedAccount linkedAccount = new LinkedAccount(
                newUser,
                provider,
                profile.providerUserId(),
                normalizedEmail
        );
        linkedAccountRepository.save(linkedAccount);
        log.info("Successfully linked OAuth provider [{}] to existing user ID: {}", provider, newUser.getId());
        return newUser;
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
            log.warn("OAuth profile rejected: Missing 'sub' (providerUserId) from Google. Attributes keys: {}", attributes.keySet());
            throw new SocialAuthException(
                    ErrorCode.AUTH_SOCIAL_PROFILE_INVALID
            );
        }

        if (email == null || email.isBlank()) {
            log.warn("OAuth profile rejected: Missing 'email' from Google for sub: {}", providerUserId);
            throw new SocialAuthException(
                    ErrorCode.AUTH_SOCIAL_EMAIL_MISSING
            );
        }

        if (!emailVerified) {
            log.warn("OAuth profile rejected: Google email is not verified for sub: {}, email: {}", providerUserId, email);
            throw new SocialAuthException(
                    ErrorCode.AUTH_SOCIAL_EMAIL_NOT_VERIFIED
            );
        }

        String normalizedEmail = EmailUtil.normalize(email);

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

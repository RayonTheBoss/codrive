package com.covoiturage.api.security;

import com.covoiturage.model.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class AuthSessionStore {
    private final Map<String, Session> sessions = new ConcurrentHashMap<>();
    private final SecureRandom random = new SecureRandom();
    private final Duration sessionDuration;

    public AuthSessionStore(@Value("${codrive.auth.session-duration:PT8H}") String sessionDuration) {
        this.sessionDuration = Duration.parse(sessionDuration);
        if (this.sessionDuration.isNegative() || this.sessionDuration.isZero()) {
            throw new IllegalArgumentException("La durée d'une session doit être positive.");
        }
    }

    public Session create(User user) {
        Instant now = Instant.now();
        sessions.values().removeIf(session -> !session.expiresAt().isAfter(now));
        byte[] secret = new byte[32];
        random.nextBytes(secret);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(secret);
        Session session = new Session(token, now.plus(sessionDuration), user.getId(), user.getSessionVersion());
        sessions.put(token, session);
        return session;
    }

    public Optional<Session> find(String token) {
        Session session = sessions.get(token);
        if (session == null) return Optional.empty();
        if (!session.expiresAt().isAfter(Instant.now())) {
            sessions.remove(token, session);
            return Optional.empty();
        }
        return Optional.of(session);
    }

    public void revoke(String token) {
        sessions.remove(token);
    }

    public record Session(String token, Instant expiresAt, int userId, int accountVersion) {}
}

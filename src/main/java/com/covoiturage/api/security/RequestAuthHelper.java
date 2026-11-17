package com.covoiturage.api.security;

import com.covoiturage.enums.StatutCompte;
import com.covoiturage.model.Admin;
import com.covoiturage.model.Chauffeur;
import com.covoiturage.model.Passager;
import com.covoiturage.model.User;
import com.covoiturage.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class RequestAuthHelper {

    private final AuthService authService;
    private final AuthSessionStore sessionStore;

    public RequestAuthHelper(AuthService authService, AuthSessionStore sessionStore) {
        this.authService = authService;
        this.sessionStore = sessionStore;
    }

    public User requireAuthenticatedUser(HttpServletRequest request) {
        String token = requireBearerToken(request);
        AuthSessionStore.Session session = sessionStore.find(token).orElseThrow(this::unauthorized);
        return authService.findUserById(session.userId())
                .filter(user -> user.getStatutCompte() == StatutCompte.ACTIF
                        && user.getSessionVersion() == session.accountVersion())
                .orElseThrow(() -> {
                    sessionStore.revoke(token);
                    return unauthorized();
                });
    }

    public void logout(HttpServletRequest request) {
        sessionStore.revoke(requireBearerToken(request));
    }

    public Passager requirePassager(HttpServletRequest request) {
        User user = requireAuthenticatedUser(request);
        if (!(user instanceof Passager passager)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Seul un passager connecté peut effectuer une réservation.");
        }
        return passager;
    }

    public Chauffeur requireChauffeur(HttpServletRequest request) {
        User user = requireAuthenticatedUser(request);
        if (!(user instanceof Chauffeur chauffeur)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Seul un chauffeur connecté peut gérer les trajets.");
        }
        return chauffeur;
    }

    public Admin requireAdmin(HttpServletRequest request) {
        User user = requireAuthenticatedUser(request);
        if (!(user instanceof Admin admin)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Seul un administrateur connecté peut accéder à cette ressource.");
        }
        return admin;
    }

    public String resolveRole(User user) {
        if (user instanceof Chauffeur) return "CHAUFFEUR";
        if (user instanceof Passager) return "PASSAGER";
        if (user instanceof Admin) return "ADMIN";
        return "USER";
    }

    private String requireBearerToken(HttpServletRequest request) {
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        String scheme = "Bearer ";
        if (authorization == null || !authorization.regionMatches(true, 0, scheme, 0, scheme.length())) {
            throw unauthorized();
        }
        String token = authorization.substring(scheme.length()).strip();
        if (token.isEmpty()) throw unauthorized();
        return token;
    }

    private ResponseStatusException unauthorized() {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Session absente, expirée ou invalide.");
    }
}

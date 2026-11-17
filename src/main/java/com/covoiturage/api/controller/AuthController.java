package com.covoiturage.api.controller;

import com.covoiturage.api.dto.request.LoginRequest;
import com.covoiturage.api.dto.request.RegisterRequest;
import com.covoiturage.api.dto.response.LoginResponse;
import com.covoiturage.api.dto.response.UserResponse;
import com.covoiturage.api.security.AuthSessionStore;
import com.covoiturage.api.security.RequestAuthHelper;
import com.covoiturage.exception.BusinessException;
import com.covoiturage.model.Chauffeur;
import com.covoiturage.model.Passager;
import com.covoiturage.model.User;
import com.covoiturage.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;
    private final AuthSessionStore sessionStore;
    private final RequestAuthHelper requestAuthHelper;

    public AuthController(AuthService authService, AuthSessionStore sessionStore, RequestAuthHelper requestAuthHelper) {
        this.authService = authService;
        this.sessionStore = sessionStore;
        this.requestAuthHelper = requestAuthHelper;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        User user = authService.authentifier(request.getEmail(), request.getMotDePasse())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                        "Email ou mot de passe incorrect."));
        return toLoginResponse(user, "Connexion réussie.");
    }

    @PostMapping("/register")
    public ResponseEntity<LoginResponse> register(@Valid @RequestBody RegisterRequest request) {
        String role = request.getRole().strip().toUpperCase(Locale.ROOT);
        User user = switch (role) {
            case "PASSAGER" -> new Passager(
                    request.getNom(),
                    request.getPrenom(),
                    request.getEmail(),
                    request.getMotDePasse(),
                    request.getTelephone()
            );
            case "CHAUFFEUR" -> new Chauffeur(
                    request.getNom(),
                    request.getPrenom(),
                    request.getEmail(),
                    request.getMotDePasse(),
                    request.getTelephone()
            );
            default -> throw new BusinessException("Rôle invalide. Utilisez PASSAGER ou CHAUFFEUR.");
        };

        authService.creerCompte(user);
        return toLoginResponse(user, "Compte créé avec succès.");
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> me(HttpServletRequest request) {
        User user = requestAuthHelper.requireAuthenticatedUser(request);
        UserResponse profile = new UserResponse(
                user.getId(),
                user.getNom(),
                user.getPrenom(),
                user.getEmail(),
                requestAuthHelper.resolveRole(user),
                user.getStatutCompte().name()
        );
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(profile);
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(HttpServletRequest request) {
        requestAuthHelper.logout(request);
    }

    private ResponseEntity<LoginResponse> toLoginResponse(User user, String message) {
        AuthSessionStore.Session session = sessionStore.create(user);
        LoginResponse body = new LoginResponse(
                user.getId(),
                user.getNom(),
                user.getPrenom(),
                user.getEmail(),
                requestAuthHelper.resolveRole(user),
                message,
                session.token(),
                session.expiresAt()
        );
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(body);
    }
}

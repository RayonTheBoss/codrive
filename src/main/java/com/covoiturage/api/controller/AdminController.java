package com.covoiturage.api.controller;

import com.covoiturage.api.dto.response.ApiMessageResponse;
import com.covoiturage.api.dto.response.TrajetResponse;
import com.covoiturage.api.dto.response.UserResponse;
import com.covoiturage.api.security.RequestAuthHelper;
import com.covoiturage.enums.StatutCompte;
import com.covoiturage.model.Admin;
import com.covoiturage.service.AuthService;
import com.covoiturage.service.TrajetService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AuthService authService;
    private final TrajetService trajetService;
    private final RequestAuthHelper requestAuthHelper;

    public AdminController(AuthService authService,
                           TrajetService trajetService,
                           RequestAuthHelper requestAuthHelper) {
        this.authService = authService;
        this.trajetService = trajetService;
        this.requestAuthHelper = requestAuthHelper;
    }

    @GetMapping("/users")
    public List<UserResponse> getUsers(HttpServletRequest request) {
        requestAuthHelper.requireAdmin(request);

        return authService.getUsers().stream()
                .map(user -> new UserResponse(
                        user.getId(),
                        user.getNom(),
                        user.getPrenom(),
                        user.getEmail(),
                        requestAuthHelper.resolveRole(user),
                        user.getStatutCompte().name()
                ))
                .toList();
    }

    @PostMapping("/users/{id}/suspend")
    public ApiMessageResponse suspendUser(@PathVariable int id, HttpServletRequest request) {
        requireOtherUser(id, request);
        authService.changeAccountStatus(id, StatutCompte.SUSPENDU);
        return new ApiMessageResponse("Utilisateur suspendu avec succès.");
    }

    @PostMapping("/users/{id}/block")
    public ApiMessageResponse blockUser(@PathVariable int id, HttpServletRequest request) {
        requireOtherUser(id, request);
        authService.changeAccountStatus(id, StatutCompte.BLOQUE);
        return new ApiMessageResponse("Utilisateur bloqué avec succès.");
    }

    @PostMapping("/users/{id}/reactivate")
    public ApiMessageResponse reactivateUser(@PathVariable int id, HttpServletRequest request) {
        requestAuthHelper.requireAdmin(request);
        authService.changeAccountStatus(id, StatutCompte.ACTIF);
        return new ApiMessageResponse("Utilisateur réactivé avec succès.");
    }

    @GetMapping("/trajets")
    public List<TrajetResponse> getAllTrips(HttpServletRequest request) {
        requestAuthHelper.requireAdmin(request);

        return trajetService.getTrajets().stream()
                .map(TrajetResponse::from)
                .toList();
    }

    @PostMapping("/trajets/{id}/cancel")
    public ApiMessageResponse cancelTripAsAdmin(@PathVariable int id, HttpServletRequest request) {
        Admin admin = requestAuthHelper.requireAdmin(request);
        return new ApiMessageResponse(trajetService.annulerTrajet(id, admin));
    }

    private void requireOtherUser(int id, HttpServletRequest request) {
        Admin admin = requestAuthHelper.requireAdmin(request);
        if (admin.getId() == id) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Vous ne pouvez pas suspendre ou bloquer votre propre compte.");
        }
    }
}

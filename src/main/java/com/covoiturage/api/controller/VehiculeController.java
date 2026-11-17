package com.covoiturage.api.controller;

import com.covoiturage.api.dto.response.VehiculeResponse;
import com.covoiturage.api.security.RequestAuthHelper;
import com.covoiturage.model.Chauffeur;
import com.covoiturage.model.Vehicule;
import com.covoiturage.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/vehicules")
public class VehiculeController {
    private final RequestAuthHelper requestAuthHelper;
    private final AuthService authService;

    public VehiculeController(RequestAuthHelper requestAuthHelper, AuthService authService) {
        this.requestAuthHelper = requestAuthHelper;
        this.authService = authService;
    }

    @GetMapping("/mine")
    public List<VehiculeResponse> getMyVehicules(HttpServletRequest request) {
        Chauffeur chauffeur = requestAuthHelper.requireChauffeur(request);
        return chauffeur.getVehicules().stream()
                .map(this::toResponse)
                .toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public VehiculeResponse createVehicule(@RequestBody VehiculeResponse request, HttpServletRequest httpRequest) {
        Chauffeur chauffeur = requestAuthHelper.requireChauffeur(httpRequest);
        Vehicule vehicule = new Vehicule(request.getMarque(), request.getModele(), request.getImmatriculation(), request.getNbPlaces());
        chauffeur.ajouterVehicule(vehicule);
        authService.saveUser(chauffeur);

        Vehicule saved = chauffeur.getVehicules().stream()
                .filter(v -> v.getImmatriculation().equalsIgnoreCase(request.getImmatriculation()))
                .findFirst()
                .orElse(vehicule);
        return toResponse(saved);
    }

    private VehiculeResponse toResponse(Vehicule v) {
        return new VehiculeResponse(v.getIdVehicule(), v.getMarque(), v.getModele(), v.getImmatriculation(), v.getNbPlaces());
    }
}

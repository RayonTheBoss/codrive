package com.covoiturage.api.controller;

import com.covoiturage.api.dto.request.CreateTrajetRequest;
import com.covoiturage.api.dto.response.ApiMessageResponse;
import com.covoiturage.api.dto.response.TrajetResponse;
import com.covoiturage.api.security.RequestAuthHelper;
import com.covoiturage.exception.BusinessException;
import com.covoiturage.model.Chauffeur;
import com.covoiturage.model.Trajet;
import com.covoiturage.model.Vehicule;
import com.covoiturage.service.TrajetService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;

@RestController
@RequestMapping("/api/trajets")
public class TrajetController {
    private final TrajetService trajetService;
    private final RequestAuthHelper requestAuthHelper;

    public TrajetController(TrajetService trajetService, RequestAuthHelper requestAuthHelper) {
        this.trajetService = trajetService;
        this.requestAuthHelper = requestAuthHelper;
    }

    @GetMapping
    public List<TrajetResponse> getTrajets(
            @RequestParam(required = false) String depart,
            @RequestParam(required = false) String arrivee
    ) {
        return trajetService.getTrajetsDisponibles().stream()
                .filter(trajet -> depart == null || trajet.getVilleDepart().equalsIgnoreCase(depart.trim()))
                .filter(trajet -> arrivee == null || trajet.getVilleArrivee().equalsIgnoreCase(arrivee.trim()))
                .map(TrajetResponse::from)
                .toList();
    }

    @GetMapping("/mine")
    public List<TrajetResponse> getMesTrajets(HttpServletRequest request) {
        Chauffeur chauffeur = requestAuthHelper.requireChauffeur(request);
        return trajetService.getTrajetsChauffeur(chauffeur.getId()).stream()
                .map(TrajetResponse::from)
                .toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public TrajetResponse createTrajet(@Valid @RequestBody CreateTrajetRequest request,
                                      HttpServletRequest httpRequest) {
        Chauffeur chauffeur = requestAuthHelper.requireChauffeur(httpRequest);
        LocalDateTime dateDepart;
        try {
            dateDepart = LocalDateTime.parse(request.getDateDepart());
        } catch (DateTimeParseException exception) {
            throw new BusinessException("La date de départ doit être au format ISO local.");
        }
        Vehicule vehicule = resolveVehicule(request, chauffeur);
        if (request.getNbPlacesTotal() > vehicule.getNbPlaces()) {
            throw new BusinessException("Le nombre de places du trajet dépasse la capacité du véhicule sélectionné.");
        }

        Trajet trajet = new Trajet(
                request.getVilleDepart().trim(),
                request.getVilleArrivee().trim(),
                dateDepart,
                request.getPrixPlace(),
                request.getNbPlacesTotal(),
                chauffeur,
                vehicule
        );
        trajetService.proposerTrajet(trajet);
        return TrajetResponse.from(trajet);
    }

    @PostMapping("/{id}/cancel")
    public ApiMessageResponse cancelTrajet(@PathVariable int id, HttpServletRequest request) {
        Chauffeur chauffeur = requestAuthHelper.requireChauffeur(request);
        return new ApiMessageResponse(trajetService.annulerTrajet(id, chauffeur));
    }

    private Vehicule resolveVehicule(CreateTrajetRequest request, Chauffeur chauffeur) {
        if (request.getIdVehicule() != null) {
            return chauffeur.getVehicules().stream()
                    .filter(vehicule -> vehicule.getIdVehicule().equals(request.getIdVehicule()))
                    .findFirst()
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN,
                            "Le véhicule sélectionné n'appartient pas au chauffeur connecté."));
        }

        if (isBlank(request.getVehiculeMarque()) || isBlank(request.getVehiculeModele())
                || isBlank(request.getVehiculeImmatriculation())) {
            throw new BusinessException("Veuillez sélectionner un véhicule existant ou saisir un nouveau véhicule complet.");
        }

        String immatriculation = request.getVehiculeImmatriculation().trim();
        return chauffeur.getVehicules().stream()
                .filter(vehicule -> immatriculation.equalsIgnoreCase(vehicule.getImmatriculation()))
                .findFirst()
                .orElseGet(() -> {
                    Vehicule vehicule = new Vehicule(request.getVehiculeMarque().trim(), request.getVehiculeModele().trim(),
                            immatriculation, request.getNbPlacesTotal());
                    chauffeur.ajouterVehicule(vehicule);
                    return vehicule;
                });
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}

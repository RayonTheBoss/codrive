package com.covoiturage.api.controller;

import com.covoiturage.api.dto.request.CreateEvaluationRequest;
import com.covoiturage.api.dto.request.CreateReservationRequest;
import com.covoiturage.api.dto.response.ApiMessageResponse;
import com.covoiturage.api.dto.response.ReservationDetailsResponse;
import com.covoiturage.api.dto.response.ReservationResponse;
import com.covoiturage.api.security.RequestAuthHelper;
import com.covoiturage.enums.StatutReservation;
import com.covoiturage.model.Chauffeur;
import com.covoiturage.model.Passager;
import com.covoiturage.model.Reservation;
import com.covoiturage.service.ReservationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/reservations")
public class ReservationController {
    private final ReservationService reservationService;
    private final RequestAuthHelper requestAuthHelper;

    public ReservationController(ReservationService reservationService, RequestAuthHelper requestAuthHelper) {
        this.reservationService = reservationService;
        this.requestAuthHelper = requestAuthHelper;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReservationResponse createReservation(@Valid @RequestBody CreateReservationRequest request,
                                                 HttpServletRequest httpRequest) {
        Passager passager = requestAuthHelper.requirePassager(httpRequest);
        if (request.getPassagerEmail() != null && !request.getPassagerEmail().isBlank()
                && !passager.getEmail().equalsIgnoreCase(request.getPassagerEmail())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "L'email de réservation doit correspondre à l'utilisateur connecté.");
        }

        Reservation reservation = reservationService.creerReservation(
                passager.getId(), request.getIdTrajet(), request.getNbPlaces(), request.getMoyenPaiement());
        return new ReservationResponse(
                reservation.getIdReservation(),
                reservation.getStatut().name(),
                reservation.getMontantTotal(),
                passager.getPrenom() + " " + passager.getNom(),
                request.getIdTrajet(),
                "Réservation créée. Paiement autorisé, en attente d'acceptation par le chauffeur."
        );
    }

    @PostMapping("/{id}/accept")
    public ApiMessageResponse acceptReservation(@PathVariable int id, HttpServletRequest request) {
        Chauffeur chauffeur = requestAuthHelper.requireChauffeur(request);
        reservationService.confirmerReservation(id, chauffeur.getId());
        return new ApiMessageResponse("Réservation acceptée. Paiement capturé.");
    }

    @PostMapping("/{id}/cancel-by-passenger")
    public ApiMessageResponse cancelByPassenger(@PathVariable int id, HttpServletRequest request) {
        Passager passager = requestAuthHelper.requirePassager(request);
        return new ApiMessageResponse(reservationService.annulerReservation(id, passager.getId()));
    }

    @PostMapping("/{id}/evaluate")
    public ApiMessageResponse evaluateDriver(@PathVariable int id,
                                             @Valid @RequestBody CreateEvaluationRequest request,
                                             HttpServletRequest httpRequest) {
        Passager passager = requestAuthHelper.requirePassager(httpRequest);
        reservationService.evaluerChauffeur(id, passager.getId(), request.getNote(), request.getCommentaire());
        return new ApiMessageResponse("Évaluation enregistrée avec succès.");
    }

    @GetMapping("/mine")
    public List<ReservationDetailsResponse> getMyReservations(HttpServletRequest request) {
        Passager passager = requestAuthHelper.requirePassager(request);
        return reservationService.getReservationsPassager(passager.getId()).stream()
                .map(reservation -> toDetailsResponse(reservation, true))
                .toList();
    }

    @GetMapping("/chauffeur")
    public List<ReservationDetailsResponse> getDriverReservations(HttpServletRequest request) {
        Chauffeur chauffeur = requestAuthHelper.requireChauffeur(request);
        return reservationService.getReservationsChauffeur(chauffeur.getId()).stream()
                .map(reservation -> toDetailsResponse(reservation, false))
                .toList();
    }

    private ReservationDetailsResponse toDetailsResponse(Reservation reservation, boolean vuePassager) {
        var trajet = reservation.getTrajet();
        var paiement = reservation.getPaiement();
        StatutReservation statut = reservation.getStatut();
        boolean avantDepart = trajet.peutGererReservations(LocalDateTime.now());
        boolean canCancel = vuePassager && avantDepart && reservation.estActive();
        boolean canEvaluate = vuePassager && reservation.peutEvaluerChauffeur();
        boolean canAccept = !vuePassager && avantDepart && statut == StatutReservation.EN_ATTENTE
                && paiement != null && paiement.isAutorise();
        return new ReservationDetailsResponse(
                reservation.getIdReservation(),
                statut.name(),
                reservation.getNbPlacesReservees(),
                reservation.getMontantTotal(),
                reservation.getDateReservation().toString(),
                trajet.getDateDepart().toString(),
                trajet.getVilleDepart(),
                trajet.getVilleArrivee(),
                reservation.getPassager().getPrenom() + " " + reservation.getPassager().getNom(),
                reservation.getPassager().getEmail(),
                paiement != null ? paiement.getType().name() : null,
                canCancel,
                canEvaluate,
                canAccept
        );
    }
}

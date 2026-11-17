package com.covoiturage.service;

import com.covoiturage.enums.StatutReservation;
import com.covoiturage.enums.TypePaiement;
import com.covoiturage.model.Chauffeur;
import com.covoiturage.model.Evaluation;
import com.covoiturage.model.Paiement;
import com.covoiturage.model.Passager;
import com.covoiturage.model.Reservation;
import com.covoiturage.model.Trajet;
import com.covoiturage.repository.ReservationRepository;
import com.covoiturage.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class ReservationService implements IReservationService {
    private final ReservationRepository reservationRepository;
    private final UserRepository userRepository;
    private final TrajetService trajetService;
    private final PaiementService paiementService;
    private final NotificationService notificationService;

    public ReservationService(ReservationRepository reservationRepository, UserRepository userRepository,
                              TrajetService trajetService, PaiementService paiementService,
                              NotificationService notificationService) {
        this.reservationRepository = reservationRepository;
        this.userRepository = userRepository;
        this.trajetService = trajetService;
        this.paiementService = paiementService;
        this.notificationService = notificationService;
    }

    @Override
    public Reservation creerReservation(int passagerId, int trajetId, int nbPlaces, TypePaiement typePaiement) {
        if (nbPlaces < 1 || typePaiement == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Données de réservation invalides.");
        }
        Trajet trajet = trajetService.verrouillerTrajet(trajetId);
        verifierTrajetAvantDepart(trajet);

        if (reservationRepository.existsByPassagerIdAndTrajetIdTrajetAndStatutIn(
                passagerId, trajetId, List.of(StatutReservation.EN_ATTENTE, StatutReservation.CONFIRMEE))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Vous avez déjà une réservation active pour ce trajet.");
        }
        if (nbPlaces > trajet.getNbPlacesDisponibles()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Pas assez de places disponibles.");
        }

        Passager passager = userRepository.findById(passagerId)
                .filter(Passager.class::isInstance)
                .map(Passager.class::cast)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Compte passager introuvable."));
        Reservation reservation = new Reservation(passager, trajet, nbPlaces);
        if (!Double.isFinite(reservation.getMontantTotal())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Montant de réservation invalide.");
        }
        Paiement paiement = new Paiement(reservation.getMontantTotal(), typePaiement);
        if (!paiementService.autoriserPaiement(paiement)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Le paiement n'a pas été autorisé.");
        }
        reservation.setPaiement(paiement);
        trajet.ajouterPassager(reservation);
        reservationRepository.save(reservation);

        notificationService.notifierEmail(passager,
                "Votre réservation est en attente d'acceptation pour le trajet " + trajet.getLibelle());
        notificationService.notifierSMS(trajet.getChauffeur(),
                "Nouvelle demande de réservation en attente sur votre trajet " + trajet.getLibelle());
        return reservation;
    }

    @Override
    public void confirmerReservation(int reservationId, int chauffeurId) {
        Reservation reservation = verrouillerReservation(reservationId);
        Trajet trajet = reservation.getTrajet();
        if (trajet.getChauffeur().getId() != chauffeurId) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Vous ne pouvez accepter que les réservations de vos trajets.");
        }
        verifierTrajetAvantDepart(trajet);
        if (reservation.getStatut() != StatutReservation.EN_ATTENTE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Seules les réservations en attente peuvent être acceptées.");
        }
        Paiement paiement = reservation.getPaiement();
        if (paiement == null || !paiement.isAutorise()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Aucun paiement autorisé pour cette réservation.");
        }
        paiementService.capturerPaiement(paiement);
        reservation.confirmerReservation();
        notificationService.notifierEmail(reservation.getPassager(),
                "Votre réservation a été acceptée par le chauffeur pour le trajet " + trajet.getLibelle());
    }

    @Override
    public String annulerReservation(int reservationId, int passagerId) {
        Reservation reservation = verrouillerReservation(reservationId);
        verifierProprietaire(reservation, passagerId);
        Trajet trajet = reservation.getTrajet();
        LocalDateTime maintenant = verifierTrajetAvantDepart(trajet);
        if (!reservation.estActive()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cette réservation n'est plus active.");
        }

        double taux = trajet.getDateDepart().isAfter(maintenant.plusHours(24)) ? 1.0 : 0.5;
        Paiement paiement = reservation.getPaiement();
        PaiementService.Annulation annulation = paiementService.reglerAnnulation(paiement, taux, 0);
        reservation.annulerReservation();
        trajet.actualiserDisponibilite();
        notificationService.notifierSMS(trajet.getChauffeur(),
                "Le passager a annulé une réservation sur votre trajet " + trajet.getLibelle());

        if (paiement == null) {
            return "Réservation annulée.";
        }
        if (!paiement.isCapture()) {
            return "Réservation annulée. Autorisation de paiement libérée.";
        }
        return "Réservation annulée. Remboursement " + (int) (taux * 100) + "% soit "
                + annulation.remboursement() + " TND.";
    }

    public void evaluerChauffeur(int reservationId, int passagerId, int note, String commentaire) {
        Reservation reservation = verrouillerReservation(reservationId);
        verifierProprietaire(reservation, passagerId);
        if (!reservation.peutEvaluerChauffeur()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Seule une réservation confirmée et sans évaluation peut être évaluée.");
        }

        Chauffeur chauffeur = reservation.getTrajet().getChauffeur();
        chauffeur.ajouterEvaluation(new Evaluation(note, commentaire));
        reservation.marquerChauffeurEvalue();
        notificationService.notifierEmail(chauffeur, "Vous avez reçu une nouvelle évaluation.");
    }

    @Override
    @Transactional(readOnly = true)
    public List<Reservation> getReservationsPassager(int passagerId) {
        return reservationRepository.findByPassagerId(passagerId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Reservation> getReservationsChauffeur(int chauffeurId) {
        return reservationRepository.findByTrajetChauffeurId(chauffeurId);
    }

    private Reservation verrouillerReservation(int id) {
        // Resolve the parent without loading a possibly stale reservation before acquiring its lock.
        int trajetId = reservationRepository.findTrajetIdByReservationId(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Réservation introuvable."));
        trajetService.verrouillerTrajet(trajetId);
        return reservationRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Réservation introuvable."));
    }

    private void verifierProprietaire(Reservation reservation, int passagerId) {
        if (reservation.getPassager().getId() != passagerId) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cette réservation appartient à un autre passager.");
        }
    }

    private LocalDateTime verifierTrajetAvantDepart(Trajet trajet) {
        LocalDateTime maintenant = LocalDateTime.now();
        if (!trajet.peutGererReservations(maintenant)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Le trajet est annulé ou a déjà débuté.");
        }
        return maintenant;
    }
}

package com.covoiturage.service;

import com.covoiturage.enums.StatutTrajet;
import com.covoiturage.exception.BusinessException;
import com.covoiturage.model.Admin;
import com.covoiturage.model.Chauffeur;
import com.covoiturage.model.Reservation;
import com.covoiturage.model.Trajet;
import com.covoiturage.model.User;
import com.covoiturage.repository.TrajetRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class TrajetService implements ITrajetService {
    private final TrajetRepository trajetRepository;
    private final PaiementService paiementService;
    private final NotificationService notificationService;

    public TrajetService(TrajetRepository trajetRepository, PaiementService paiementService,
                         NotificationService notificationService) {
        this.trajetRepository = trajetRepository;
        this.paiementService = paiementService;
        this.notificationService = notificationService;
    }

    @Override
    public void proposerTrajet(Trajet trajet) {
        if (trajet == null || trajet.getChauffeur() == null || trajet.getVehicule() == null) {
            throw new BusinessException("Trajet invalide.");
        }
        if (!trajet.getDateDepart().isAfter(LocalDateTime.now())) {
            throw new BusinessException("La date de départ doit être dans le futur.");
        }
        if (!Double.isFinite(trajet.getPrixPlace()) || trajet.getPrixPlace() <= 0) {
            throw new BusinessException("Le prix par place doit être un montant positif.");
        }

        boolean duplicateExists = trajetRepository
                .existsByChauffeurIdAndVilleDepartIgnoreCaseAndVilleArriveeIgnoreCaseAndDateDepartAndPrixPlaceAndVehiculeIdVehicule(
                        trajet.getChauffeur().getId(),
                        trajet.getVilleDepart(),
                        trajet.getVilleArrivee(),
                        trajet.getDateDepart(),
                        trajet.getPrixPlace(),
                        trajet.getVehicule().getIdVehicule()
                );
        if (duplicateExists) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Un trajet identique existe déjà pour ce chauffeur et ce véhicule.");
        }

        trajetRepository.save(trajet);
        notificationService.notifierEmail(trajet.getChauffeur(), "Votre trajet " + trajet.getLibelle() + " a été publié.");
    }

    @Override
    public String annulerTrajet(int id, User acteur) {
        Trajet trajet = verrouillerTrajet(id);
        boolean admin = acteur instanceof Admin;
        if (!admin && (!(acteur instanceof Chauffeur) || !trajet.getChauffeur().getId().equals(acteur.getId()))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Vous ne pouvez annuler que vos propres trajets.");
        }
        if (trajet.getStatut() == StatutTrajet.ANNULE) {
            return "Ce trajet est déjà annulé.";
        }

        LocalDateTime maintenant = LocalDateTime.now();
        if (!trajet.peutGererReservations(maintenant)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Le trajet a déjà débuté.");
        }

        List<Reservation> actives = trajet.getReservations().stream().filter(Reservation::estActive).toList();
        double taux = !actives.isEmpty() && trajet.getDateDepart().isBefore(maintenant.plusHours(24)) ? 0.20 : 0;
        String auteur = admin ? "L'administrateur" : "Le chauffeur";
        for (Reservation reservation : actives) {
            PaiementService.Annulation annulation = paiementService.reglerAnnulation(reservation.getPaiement(), 1.0, taux);
            reservation.annulerReservation();
            notificationService.notifierEmail(reservation.getPassager(),
                    auteur + " a annulé le trajet " + trajet.getLibelle() + ". Compensation : "
                            + annulation.compensation() + " TND.");
        }

        trajet.annuler();
        String messageChauffeur = admin
                ? "L'administrateur a annulé votre trajet " + trajet.getLibelle() + "."
                : "Vous avez annulé le trajet " + trajet.getLibelle() + ".";
        notificationService.notifierEmail(trajet.getChauffeur(), messageChauffeur);

        String message = admin ? "Trajet annulé par l'administrateur." : "Trajet annulé.";
        return message + " Compensation passager : " + (int) (taux * 100) + "%.";
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public Trajet verrouillerTrajet(int id) {
        return trajetRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Trajet introuvable."));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Trajet> getTrajets() {
        return trajetRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Trajet> getTrajetsDisponibles() {
        return trajetRepository.findByDateDepartAfterAndStatutNot(LocalDateTime.now(), StatutTrajet.ANNULE).stream()
                .filter(trajet -> trajet.getNbPlacesDisponibles() > 0)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Trajet> getTrajetsChauffeur(int chauffeurId) {
        return trajetRepository.findByChauffeurId(chauffeurId);
    }
}

package com.covoiturage.service;

import com.covoiturage.enums.TypePaiement;
import com.covoiturage.model.Reservation;
import java.util.List;

public interface IReservationService {
    Reservation creerReservation(int passagerId, int trajetId, int nbPlaces, TypePaiement typePaiement);
    void confirmerReservation(int reservationId, int chauffeurId);
    String annulerReservation(int reservationId, int passagerId);
    List<Reservation> getReservationsPassager(int passagerId);
    List<Reservation> getReservationsChauffeur(int chauffeurId);
}

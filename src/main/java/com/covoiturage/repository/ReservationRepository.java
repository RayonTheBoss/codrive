package com.covoiturage.repository;

import com.covoiturage.enums.StatutReservation;
import com.covoiturage.model.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ReservationRepository extends JpaRepository<Reservation, Integer> {
    boolean existsByPassagerIdAndTrajetIdTrajetAndStatutIn(int passagerId, int idTrajet, Collection<StatutReservation> statuts);

    List<Reservation> findByPassagerId(int passagerId);

    List<Reservation> findByTrajetChauffeurId(int chauffeurId);

    @Query("select reservation.trajet.idTrajet from Reservation reservation where reservation.idReservation = :id")
    Optional<Integer> findTrajetIdByReservationId(@Param("id") int id);
}

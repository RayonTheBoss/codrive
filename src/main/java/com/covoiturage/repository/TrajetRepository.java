package com.covoiturage.repository;

import com.covoiturage.enums.StatutTrajet;
import com.covoiturage.model.Trajet;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TrajetRepository extends JpaRepository<Trajet, Integer> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select trajet from Trajet trajet where trajet.idTrajet = :id")
    Optional<Trajet> findByIdForUpdate(@Param("id") int id);

    List<Trajet> findByChauffeurId(int chauffeurId);

    List<Trajet> findByDateDepartAfterAndStatutNot(LocalDateTime maintenant, StatutTrajet statut);

    boolean existsByChauffeurIdAndVilleDepartIgnoreCaseAndVilleArriveeIgnoreCaseAndDateDepartAndPrixPlaceAndVehiculeIdVehicule(
            Integer chauffeurId,
            String villeDepart,
            String villeArrivee,
            LocalDateTime dateDepart,
            double prixPlace,
            Integer idVehicule
    );
}

package com.covoiturage.model;

import com.covoiturage.enums.StatutReservation;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
public class Reservation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idReservation;
    private LocalDateTime dateReservation;
    private int nbPlacesReservees;
    private double montantTotal;

    @Enumerated(EnumType.STRING)
    private StatutReservation statut;
    private Boolean chauffeurEvalue;

    @ManyToOne
    private Passager passager;

    @ManyToOne
    private Trajet trajet;

    @OneToOne(cascade = CascadeType.ALL)
    private Paiement paiement;

    public Reservation() {}

    public Reservation(Passager passager, Trajet trajet, int nbPlacesReservees) {
        this.passager = passager;
        this.trajet = trajet;
        this.nbPlacesReservees = nbPlacesReservees;
        this.dateReservation = LocalDateTime.now();
        this.montantTotal = trajet.getPrixPlace() * nbPlacesReservees;
        this.statut = StatutReservation.EN_ATTENTE;
    }

    public Integer getIdReservation() { return idReservation; }
    public void setIdReservation(Integer idReservation) { this.idReservation = idReservation; }
    public LocalDateTime getDateReservation() { return dateReservation; }
    public void setDateReservation(LocalDateTime dateReservation) { this.dateReservation = dateReservation; }
    public int getNbPlacesReservees() { return nbPlacesReservees; }
    public void setNbPlacesReservees(int nbPlacesReservees) { this.nbPlacesReservees = nbPlacesReservees; }
    public double getMontantTotal() { return montantTotal; }
    public void setMontantTotal(double montantTotal) { this.montantTotal = montantTotal; }
    public StatutReservation getStatut() { return statut; }
    public void setStatut(StatutReservation statut) { this.statut = statut; }
    public Passager getPassager() { return passager; }
    public void setPassager(Passager passager) { this.passager = passager; }
    public Trajet getTrajet() { return trajet; }
    public void setTrajet(Trajet trajet) { this.trajet = trajet; }
    public Paiement getPaiement() { return paiement; }
    public void setPaiement(Paiement paiement) { this.paiement = paiement; }

    public boolean estActive() {
        return statut == StatutReservation.EN_ATTENTE || statut == StatutReservation.CONFIRMEE;
    }

    public boolean peutEvaluerChauffeur() {
        return statut == StatutReservation.CONFIRMEE && !Boolean.TRUE.equals(chauffeurEvalue);
    }

    public void marquerChauffeurEvalue() {
        chauffeurEvalue = true;
    }

    public void confirmerReservation() { this.statut = StatutReservation.CONFIRMEE; }
    public void annulerReservation() { this.statut = StatutReservation.ANNULEE; }
    public void rembourserReservation() { this.statut = StatutReservation.REMBOURSEE; }

    @Override
    public String toString() { return "Reservation{idReservation=" + idReservation + ", nbPlacesReservees=" + nbPlacesReservees + ", montantTotal=" + montantTotal + ", statut=" + statut + "}"; }
    @Override
    public boolean equals(Object o) {
        return this == o || (o instanceof Reservation that && idReservation != null && idReservation.equals(that.getIdReservation()));
    }
    @Override
    public int hashCode() { return Reservation.class.hashCode(); }
}

package com.covoiturage.model;

import com.covoiturage.enums.StatutTrajet;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
public class Trajet {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idTrajet;
    private String villeDepart;
    private String villeArrivee;
    private LocalDateTime dateDepart;
    private double prixPlace;
    private int nbPlacesTotal;
    private int nbPlacesDisponibles;

    @Enumerated(EnumType.STRING)
    private StatutTrajet statut;

    @ManyToOne
    private Chauffeur chauffeur;

    @ManyToOne(cascade = CascadeType.ALL)
    private Vehicule vehicule;

    @OneToMany(mappedBy = "trajet", cascade = CascadeType.ALL)
    private List<Reservation> reservations = new ArrayList<>();

    public Trajet() {}

    public Trajet(String villeDepart, String villeArrivee, LocalDateTime dateDepart,
                  double prixPlace, int nbPlacesTotal, Chauffeur chauffeur, Vehicule vehicule) {
        this.villeDepart = villeDepart;
        this.villeArrivee = villeArrivee;
        this.dateDepart = dateDepart;
        this.prixPlace = prixPlace;
        this.nbPlacesTotal = nbPlacesTotal;
        this.nbPlacesDisponibles = nbPlacesTotal;
        this.statut = StatutTrajet.OUVERT;
        this.chauffeur = chauffeur;
        this.vehicule = vehicule;
    }

    public Integer getIdTrajet() { return idTrajet; }
    public void setIdTrajet(Integer idTrajet) { this.idTrajet = idTrajet; }
    public String getVilleDepart() { return villeDepart; }
    public void setVilleDepart(String villeDepart) { this.villeDepart = villeDepart; }
    public String getVilleArrivee() { return villeArrivee; }
    public void setVilleArrivee(String villeArrivee) { this.villeArrivee = villeArrivee; }
    public String getLibelle() { return villeDepart + " -> " + villeArrivee; }
    public LocalDateTime getDateDepart() { return dateDepart; }
    public void setDateDepart(LocalDateTime dateDepart) { this.dateDepart = dateDepart; }
    public double getPrixPlace() { return prixPlace; }
    public void setPrixPlace(double prixPlace) { this.prixPlace = prixPlace; }
    public int getNbPlacesTotal() { return nbPlacesTotal; }
    public void setNbPlacesTotal(int nbPlacesTotal) { this.nbPlacesTotal = nbPlacesTotal; }
    public int getNbPlacesDisponibles() {
        // Recompute from reservations so historic cancellations cannot inflate inventory.
        int placesReservees = reservations.stream()
                .filter(Reservation::estActive)
                .mapToInt(Reservation::getNbPlacesReservees)
                .sum();
        return Math.max(0, nbPlacesTotal - placesReservees);
    }

    public StatutTrajet getStatut() {
        if (statut == StatutTrajet.ANNULE) return statut;
        return getNbPlacesDisponibles() == 0 ? StatutTrajet.COMPLET : StatutTrajet.OUVERT;
    }
    public Chauffeur getChauffeur() { return chauffeur; }
    public void setChauffeur(Chauffeur chauffeur) { this.chauffeur = chauffeur; }
    public Vehicule getVehicule() { return vehicule; }
    public void setVehicule(Vehicule vehicule) { this.vehicule = vehicule; }
    public List<Reservation> getReservations() { return new ArrayList<>(reservations); }

    public boolean peutGererReservations(LocalDateTime maintenant) {
        return statut != StatutTrajet.ANNULE && dateDepart.isAfter(maintenant);
    }

    public void ajouterPassager(Reservation reservation) {
        reservations.add(reservation);
        actualiserDisponibilite();
    }

    public void actualiserDisponibilite() {
        nbPlacesDisponibles = getNbPlacesDisponibles();
        if (statut != StatutTrajet.ANNULE) {
            statut = nbPlacesDisponibles == 0 ? StatutTrajet.COMPLET : StatutTrajet.OUVERT;
        }
    }

    public void annuler() {
        statut = StatutTrajet.ANNULE;
        actualiserDisponibilite();
    }

    @Override
    public String toString() { return "Trajet{idTrajet=" + idTrajet + ", villeDepart='" + villeDepart + "', villeArrivee='" + villeArrivee + "', dateDepart=" + dateDepart + ", prixPlace=" + prixPlace + ", statut=" + statut + "}"; }
    @Override
    public boolean equals(Object o) {
        return this == o || (o instanceof Trajet trajet && idTrajet != null && idTrajet.equals(trajet.getIdTrajet()));
    }
    @Override
    public int hashCode() { return Trajet.class.hashCode(); }
}

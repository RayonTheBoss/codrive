package com.covoiturage.model;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@DiscriminatorValue("PASSAGER")
public class Passager extends User {
    @OneToMany(mappedBy = "passager", cascade = CascadeType.ALL)
    private List<Reservation> reservations = new ArrayList<>();

    public Passager() {}

    public Passager(String nom, String prenom, String email, String motDePasse, String telephone) {
        super(nom, prenom, email, motDePasse, telephone);
    }

    public void ajouterReservation(Reservation reservation) {
        if (reservation != null && !this.reservations.contains(reservation)) this.reservations.add(reservation);
    }

    public List<Reservation> consulterReservations() { return new ArrayList<>(reservations); }
}

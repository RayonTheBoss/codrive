package com.covoiturage.model;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("ADMIN")
public class Admin extends User {
    public Admin() {}

    public Admin(String nom, String prenom, String email, String motDePasse, String telephone) {
        super(nom, prenom, email, motDePasse, telephone);
    }

    public void suspendreCompte(User user) { user.suspendreCompte(); }
    public void bloquerUtilisateur(User user) { user.bloquerCompte(); }
}

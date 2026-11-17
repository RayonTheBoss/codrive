package com.covoiturage.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.util.Objects;

@Entity
public class Vehicule {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idVehicule;
    private String marque;
    private String modele;
    private String immatriculation;
    private int nbPlaces;

    public Vehicule() {}

    public Vehicule(String marque, String modele, String immatriculation, int nbPlaces) {
        this.marque = marque;
        this.modele = modele;
        this.immatriculation = immatriculation;
        this.nbPlaces = nbPlaces;
    }

    public Integer getIdVehicule() { return idVehicule; }
    public void setIdVehicule(Integer idVehicule) { this.idVehicule = idVehicule; }
    public String getMarque() { return marque; }
    public void setMarque(String marque) { this.marque = marque; }
    public String getModele() { return modele; }
    public void setModele(String modele) { this.modele = modele; }
    public String getImmatriculation() { return immatriculation; }
    public void setImmatriculation(String immatriculation) { this.immatriculation = immatriculation; }
    public int getNbPlaces() { return nbPlaces; }
    public void setNbPlaces(int nbPlaces) { this.nbPlaces = nbPlaces; }

    @Override
    public String toString() { return "Vehicule{idVehicule=" + idVehicule + ", marque='" + marque + "', modele='" + modele + "', immatriculation='" + immatriculation + "', nbPlaces=" + nbPlaces + "}"; }
    @Override
    public boolean equals(Object o) { return this == o || (o instanceof Vehicule vehicule && Objects.equals(idVehicule, vehicule.idVehicule)); }
    @Override
    public int hashCode() { return Objects.hash(idVehicule); }
}

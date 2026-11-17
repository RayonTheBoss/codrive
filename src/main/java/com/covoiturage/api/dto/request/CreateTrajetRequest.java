package com.covoiturage.api.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public class CreateTrajetRequest {
    @NotBlank
    private String villeDepart;

    @NotBlank
    private String villeArrivee;

    @NotBlank
    private String dateDepart;

    @DecimalMin("0.1")
    private double prixPlace;

    @Min(1)
    private int nbPlacesTotal;

    private Integer idVehicule;

    private String vehiculeMarque;
    private String vehiculeModele;
    private String vehiculeImmatriculation;

    public String getVilleDepart() { return villeDepart; }
    public void setVilleDepart(String villeDepart) { this.villeDepart = villeDepart; }
    public String getVilleArrivee() { return villeArrivee; }
    public void setVilleArrivee(String villeArrivee) { this.villeArrivee = villeArrivee; }
    public String getDateDepart() { return dateDepart; }
    public void setDateDepart(String dateDepart) { this.dateDepart = dateDepart; }
    public double getPrixPlace() { return prixPlace; }
    public void setPrixPlace(double prixPlace) { this.prixPlace = prixPlace; }
    public int getNbPlacesTotal() { return nbPlacesTotal; }
    public void setNbPlacesTotal(int nbPlacesTotal) { this.nbPlacesTotal = nbPlacesTotal; }
    public Integer getIdVehicule() { return idVehicule; }
    public void setIdVehicule(Integer idVehicule) { this.idVehicule = idVehicule; }
    public String getVehiculeMarque() { return vehiculeMarque; }
    public void setVehiculeMarque(String vehiculeMarque) { this.vehiculeMarque = vehiculeMarque; }
    public String getVehiculeModele() { return vehiculeModele; }
    public void setVehiculeModele(String vehiculeModele) { this.vehiculeModele = vehiculeModele; }
    public String getVehiculeImmatriculation() { return vehiculeImmatriculation; }
    public void setVehiculeImmatriculation(String vehiculeImmatriculation) { this.vehiculeImmatriculation = vehiculeImmatriculation; }
}

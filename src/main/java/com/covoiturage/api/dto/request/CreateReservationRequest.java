package com.covoiturage.api.dto.request;

import com.covoiturage.enums.TypePaiement;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class CreateReservationRequest {
    private String passagerNom;

    @Email
    private String passagerEmail;

    @Min(1)
    private int nbPlaces;

    @NotNull
    private TypePaiement moyenPaiement;

    @Min(1)
    private int idTrajet;

    public String getPassagerNom() { return passagerNom; }
    public void setPassagerNom(String passagerNom) { this.passagerNom = passagerNom; }
    public String getPassagerEmail() { return passagerEmail; }
    public void setPassagerEmail(String passagerEmail) { this.passagerEmail = passagerEmail; }
    public int getNbPlaces() { return nbPlaces; }
    public void setNbPlaces(int nbPlaces) { this.nbPlaces = nbPlaces; }
    public TypePaiement getMoyenPaiement() { return moyenPaiement; }
    public void setMoyenPaiement(TypePaiement moyenPaiement) { this.moyenPaiement = moyenPaiement; }
    public int getIdTrajet() { return idTrajet; }
    public void setIdTrajet(int idTrajet) { this.idTrajet = idTrajet; }
}

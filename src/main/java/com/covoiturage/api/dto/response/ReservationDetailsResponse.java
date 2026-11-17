package com.covoiturage.api.dto.response;

public class ReservationDetailsResponse {
    private int idReservation;
    private String statut;
    private int nbPlacesReservees;
    private double montantTotal;
    private String dateReservation;
    private String dateDepart;
    private String villeDepart;
    private String villeArrivee;
    private String passagerNom;
    private String passagerEmail;
    private String typePaiement;
    private boolean canCancel;
    private boolean canEvaluate;
    private boolean canAccept;

    public ReservationDetailsResponse() {}

    public ReservationDetailsResponse(int idReservation, String statut, int nbPlacesReservees, double montantTotal,
                                      String dateReservation, String dateDepart, String villeDepart, String villeArrivee,
                                      String passagerNom, String passagerEmail, String typePaiement,
                                      boolean canCancel, boolean canEvaluate, boolean canAccept) {
        this.idReservation = idReservation;
        this.statut = statut;
        this.nbPlacesReservees = nbPlacesReservees;
        this.montantTotal = montantTotal;
        this.dateReservation = dateReservation;
        this.dateDepart = dateDepart;
        this.villeDepart = villeDepart;
        this.villeArrivee = villeArrivee;
        this.passagerNom = passagerNom;
        this.passagerEmail = passagerEmail;
        this.typePaiement = typePaiement;
        this.canCancel = canCancel;
        this.canEvaluate = canEvaluate;
        this.canAccept = canAccept;
    }

    public int getIdReservation() { return idReservation; }
    public void setIdReservation(int idReservation) { this.idReservation = idReservation; }
    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }
    public int getNbPlacesReservees() { return nbPlacesReservees; }
    public void setNbPlacesReservees(int nbPlacesReservees) { this.nbPlacesReservees = nbPlacesReservees; }
    public double getMontantTotal() { return montantTotal; }
    public void setMontantTotal(double montantTotal) { this.montantTotal = montantTotal; }
    public String getDateReservation() { return dateReservation; }
    public void setDateReservation(String dateReservation) { this.dateReservation = dateReservation; }
    public String getDateDepart() { return dateDepart; }
    public void setDateDepart(String dateDepart) { this.dateDepart = dateDepart; }
    public String getVilleDepart() { return villeDepart; }
    public void setVilleDepart(String villeDepart) { this.villeDepart = villeDepart; }
    public String getVilleArrivee() { return villeArrivee; }
    public void setVilleArrivee(String villeArrivee) { this.villeArrivee = villeArrivee; }
    public String getPassagerNom() { return passagerNom; }
    public void setPassagerNom(String passagerNom) { this.passagerNom = passagerNom; }
    public String getPassagerEmail() { return passagerEmail; }
    public void setPassagerEmail(String passagerEmail) { this.passagerEmail = passagerEmail; }
    public String getTypePaiement() { return typePaiement; }
    public void setTypePaiement(String typePaiement) { this.typePaiement = typePaiement; }
    public boolean isCanCancel() { return canCancel; }
    public void setCanCancel(boolean canCancel) { this.canCancel = canCancel; }
    public boolean isCanEvaluate() { return canEvaluate; }
    public void setCanEvaluate(boolean canEvaluate) { this.canEvaluate = canEvaluate; }
    public boolean isCanAccept() { return canAccept; }
    public void setCanAccept(boolean canAccept) { this.canAccept = canAccept; }
}

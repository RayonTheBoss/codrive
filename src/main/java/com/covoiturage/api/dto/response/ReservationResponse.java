package com.covoiturage.api.dto.response;

public class ReservationResponse {
    private int idReservation;
    private String statut;
    private double montantTotal;
    private String passager;
    private int idTrajet;
    private String message;

    public ReservationResponse() {}

    public ReservationResponse(int idReservation, String statut, double montantTotal,
                               String passager, int idTrajet, String message) {
        this.idReservation = idReservation;
        this.statut = statut;
        this.montantTotal = montantTotal;
        this.passager = passager;
        this.idTrajet = idTrajet;
        this.message = message;
    }

    public int getIdReservation() { return idReservation; }
    public void setIdReservation(int idReservation) { this.idReservation = idReservation; }
    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }
    public double getMontantTotal() { return montantTotal; }
    public void setMontantTotal(double montantTotal) { this.montantTotal = montantTotal; }
    public String getPassager() { return passager; }
    public void setPassager(String passager) { this.passager = passager; }
    public int getIdTrajet() { return idTrajet; }
    public void setIdTrajet(int idTrajet) { this.idTrajet = idTrajet; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}

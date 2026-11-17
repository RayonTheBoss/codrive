package com.covoiturage.api.dto.response;

import com.covoiturage.model.Trajet;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class TrajetResponse {
    private int idTrajet;
    private String villeDepart;
    private String villeArrivee;
    private String dateDepart;
    private double prixPlace;
    private int nbPlacesDisponibles;
    private String statut;
    private boolean canCancel;
    private ChauffeurMini chauffeur;
    private VehiculeMini vehicule;

    public TrajetResponse() {}

    public TrajetResponse(int idTrajet, String villeDepart, String villeArrivee, String dateDepart,
                          double prixPlace, int nbPlacesDisponibles, String statut,
                          ChauffeurMini chauffeur, VehiculeMini vehicule) {
        this.idTrajet = idTrajet;
        this.villeDepart = villeDepart;
        this.villeArrivee = villeArrivee;
        this.dateDepart = dateDepart;
        this.prixPlace = prixPlace;
        this.nbPlacesDisponibles = nbPlacesDisponibles;
        this.statut = statut;
        this.chauffeur = chauffeur;
        this.vehicule = vehicule;
    }

    public static TrajetResponse from(Trajet trajet) {
        var chauffeur = trajet.getChauffeur();
        var vehicule = trajet.getVehicule();
        List<EvaluationMini> evaluations = chauffeur.getEvaluations().stream()
                .map(evaluation -> new EvaluationMini(evaluation.getNote(), evaluation.getCommentaire()))
                .toList();
        TrajetResponse response = new TrajetResponse(
                trajet.getIdTrajet(), trajet.getVilleDepart(), trajet.getVilleArrivee(), trajet.getDateDepart().toString(),
                trajet.getPrixPlace(), trajet.getNbPlacesDisponibles(), trajet.getStatut().name(),
                new ChauffeurMini(chauffeur.getNom(), chauffeur.getPrenom(), chauffeur.consulterNotesChauffeur(), evaluations),
                new VehiculeMini(vehicule.getMarque(), vehicule.getModele())
        );
        response.canCancel = trajet.peutGererReservations(LocalDateTime.now());
        return response;
    }

    public int getIdTrajet() { return idTrajet; }
    public void setIdTrajet(int idTrajet) { this.idTrajet = idTrajet; }
    public String getVilleDepart() { return villeDepart; }
    public void setVilleDepart(String villeDepart) { this.villeDepart = villeDepart; }
    public String getVilleArrivee() { return villeArrivee; }
    public void setVilleArrivee(String villeArrivee) { this.villeArrivee = villeArrivee; }
    public String getDateDepart() { return dateDepart; }
    public void setDateDepart(String dateDepart) { this.dateDepart = dateDepart; }
    public double getPrixPlace() { return prixPlace; }
    public void setPrixPlace(double prixPlace) { this.prixPlace = prixPlace; }
    public int getNbPlacesDisponibles() { return nbPlacesDisponibles; }
    public void setNbPlacesDisponibles(int nbPlacesDisponibles) { this.nbPlacesDisponibles = nbPlacesDisponibles; }
    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }
    public boolean isCanCancel() { return canCancel; }
    public ChauffeurMini getChauffeur() { return chauffeur; }
    public void setChauffeur(ChauffeurMini chauffeur) { this.chauffeur = chauffeur; }
    public VehiculeMini getVehicule() { return vehicule; }
    public void setVehicule(VehiculeMini vehicule) { this.vehicule = vehicule; }

    public static class ChauffeurMini {
        private String nom;
        private String prenom;
        private double noteMoyenne;
        private List<EvaluationMini> evaluations = new ArrayList<>();

        public ChauffeurMini() {}
        public ChauffeurMini(String nom, String prenom, double noteMoyenne) {
            this.nom = nom;
            this.prenom = prenom;
            this.noteMoyenne = noteMoyenne;
        }
        public ChauffeurMini(String nom, String prenom, double noteMoyenne, List<EvaluationMini> evaluations) {
            this.nom = nom;
            this.prenom = prenom;
            this.noteMoyenne = noteMoyenne;
            this.evaluations = evaluations;
        }

        public String getNom() { return nom; }
        public void setNom(String nom) { this.nom = nom; }
        public String getPrenom() { return prenom; }
        public void setPrenom(String prenom) { this.prenom = prenom; }
        public double getNoteMoyenne() { return noteMoyenne; }
        public void setNoteMoyenne(double noteMoyenne) { this.noteMoyenne = noteMoyenne; }
        public List<EvaluationMini> getEvaluations() { return evaluations; }
        public void setEvaluations(List<EvaluationMini> evaluations) { this.evaluations = evaluations; }
    }

    public static class EvaluationMini {
        private int note;
        private String commentaire;

        public EvaluationMini() {}
        public EvaluationMini(int note, String commentaire) {
            this.note = note;
            this.commentaire = commentaire;
        }

        public int getNote() { return note; }
        public void setNote(int note) { this.note = note; }
        public String getCommentaire() { return commentaire; }
        public void setCommentaire(String commentaire) { this.commentaire = commentaire; }
    }

    public static class VehiculeMini {
        private String marque;
        private String modele;

        public VehiculeMini() {}
        public VehiculeMini(String marque, String modele) {
            this.marque = marque;
            this.modele = modele;
        }

        public String getMarque() { return marque; }
        public void setMarque(String marque) { this.marque = marque; }
        public String getModele() { return modele; }
        public void setModele(String modele) { this.modele = modele; }
    }
}

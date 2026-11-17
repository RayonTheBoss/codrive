package com.covoiturage.model;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@DiscriminatorValue("CHAUFFEUR")
public class Chauffeur extends User {
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "chauffeur_id")
    private List<Vehicule> vehicules = new ArrayList<>();

    @OneToMany(mappedBy = "chauffeur", cascade = CascadeType.ALL)
    private List<Trajet> trajets = new ArrayList<>();

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "chauffeur_eval_id")
    private List<Evaluation> evaluations = new ArrayList<>();

    public Chauffeur() {}

    public Chauffeur(String nom, String prenom, String email, String motDePasse, String telephone) {
        super(nom, prenom, email, motDePasse, telephone);
    }

    public void ajouterVehicule(Vehicule vehicule) { if (vehicule != null) this.vehicules.add(vehicule); }
    public void ajouterTrajet(Trajet trajet) { if (trajet != null && !this.trajets.contains(trajet)) this.trajets.add(trajet); }
    public void ajouterEvaluation(Evaluation evaluation) { if (evaluation != null) this.evaluations.add(evaluation); }

    public List<Vehicule> getVehicules() { return new ArrayList<>(vehicules); }
    public List<Trajet> getTrajets() { return new ArrayList<>(trajets); }
    public List<Evaluation> getEvaluations() { return new ArrayList<>(evaluations); }

    public double consulterNotesChauffeur() {
        if (evaluations.isEmpty()) return 0.0;
        double somme = 0;
        for (Evaluation e : evaluations) somme += e.getNote();
        return somme / evaluations.size();
    }
}

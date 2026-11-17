package com.covoiturage.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.time.LocalDateTime;

@Entity
public class Evaluation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idEvaluation;
    private int note;
    private String commentaire;
    private LocalDateTime dateEvaluation;

    public Evaluation() {}

    public Evaluation(int note, String commentaire) {
        this.note = note;
        this.commentaire = commentaire;
        this.dateEvaluation = LocalDateTime.now();
    }

    public Integer getIdEvaluation() { return idEvaluation; }
    public void setIdEvaluation(Integer idEvaluation) { this.idEvaluation = idEvaluation; }
    public int getNote() { return note; }
    public void setNote(int note) { this.note = note; }
    public String getCommentaire() { return commentaire; }
    public void setCommentaire(String commentaire) { this.commentaire = commentaire; }
    public LocalDateTime getDateEvaluation() { return dateEvaluation; }
    public void setDateEvaluation(LocalDateTime dateEvaluation) { this.dateEvaluation = dateEvaluation; }
}

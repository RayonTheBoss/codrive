package com.covoiturage.model;

import com.covoiturage.enums.TypePaiement;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
public class Paiement {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idPaiement;
    private double montant;
    private LocalDateTime datePaiement;

    @Enumerated(EnumType.STRING)
    private TypePaiement type;

    private boolean autorise;
    private boolean capture;
    private Double montantRembourse;
    private Double montantCompensation;

    public Paiement() {}

    public Paiement(double montant, TypePaiement type) {
        this.montant = montant;
        this.type = type;
        this.datePaiement = LocalDateTime.now();
        this.autorise = false;
        this.capture = false;
    }

    public Integer getIdPaiement() { return idPaiement; }
    public void setIdPaiement(Integer idPaiement) { this.idPaiement = idPaiement; }
    public double getMontant() { return montant; }
    public void setMontant(double montant) { this.montant = montant; }
    public LocalDateTime getDatePaiement() { return datePaiement; }
    public void setDatePaiement(LocalDateTime datePaiement) { this.datePaiement = datePaiement; }
    public TypePaiement getType() { return type; }
    public void setType(TypePaiement type) { this.type = type; }
    public boolean isAutorise() { return autorise; }
    public void setAutorise(boolean autorise) { this.autorise = autorise; }
    public boolean isCapture() { return capture; }
    public void setCapture(boolean capture) { this.capture = capture; }
    public Double getMontantRembourse() { return montantRembourse; }
    public Double getMontantCompensation() { return montantCompensation; }

    public boolean payer() { this.autorise = true; return true; }
    public void capturer() { if (autorise) this.capture = true; }
    public void reglerAnnulation(double remboursement, double compensation) {
        autorise = false;
        montantRembourse = remboursement;
        montantCompensation = compensation;
    }

    @Override
    public String toString() { return "Paiement{idPaiement=" + idPaiement + ", montant=" + montant + ", type=" + type + ", autorise=" + autorise + ", capture=" + capture + "}"; }
    @Override
    public boolean equals(Object o) { return this == o || (o instanceof Paiement paiement && Objects.equals(idPaiement, paiement.idPaiement)); }
    @Override
    public int hashCode() { return Objects.hash(idPaiement); }
}

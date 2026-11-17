package com.covoiturage.model;

import com.covoiturage.enums.TypePaiement;
import jakarta.persistence.*;

@Entity
public class MoyenPaiement {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idMoyenPaiement;

    @Enumerated(EnumType.STRING)
    private TypePaiement type;

    private String details;

    public MoyenPaiement() {}

    public MoyenPaiement(TypePaiement type, String details) {
        this.type = type;
        this.details = details;
    }

    public Integer getIdMoyenPaiement() { return idMoyenPaiement; }
    public void setIdMoyenPaiement(Integer idMoyenPaiement) { this.idMoyenPaiement = idMoyenPaiement; }
    public TypePaiement getType() { return type; }
    public void setType(TypePaiement type) { this.type = type; }
    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }
}

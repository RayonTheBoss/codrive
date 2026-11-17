package com.covoiturage.api.dto.response;

public class UserResponse {
    private int id;
    private String nom;
    private String prenom;
    private String email;
    private String role;
    private String statut;

    public UserResponse() {
    }

    public UserResponse(int id, String nom, String prenom, String email, String role, String statut) {
        this.id = id;
        this.nom = nom;
        this.prenom = prenom;
        this.email = email;
        this.role = role;
        this.statut = statut;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }
    public String getPrenom() { return prenom; }
    public void setPrenom(String prenom) { this.prenom = prenom; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }
}

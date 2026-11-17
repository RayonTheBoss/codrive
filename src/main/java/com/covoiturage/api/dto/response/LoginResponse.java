package com.covoiturage.api.dto.response;

import java.time.Instant;

public class LoginResponse {
    private int id;
    private String nom;
    private String prenom;
    private String email;
    private String role;
    private String message;
    private String token;
    private Instant expiresAt;

    public LoginResponse() {}

    public LoginResponse(int id, String nom, String prenom, String email, String role, String message,
                         String token, Instant expiresAt) {
        this.id = id;
        this.nom = nom;
        this.prenom = prenom;
        this.email = email;
        this.role = role;
        this.message = message;
        this.token = token;
        this.expiresAt = expiresAt;
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
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getToken() { return token; }
    public Instant getExpiresAt() { return expiresAt; }
}

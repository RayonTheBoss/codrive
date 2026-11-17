package com.covoiturage.model;

import com.covoiturage.enums.StatutCompte;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import org.hibernate.annotations.DynamicUpdate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "users")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "user_type")
@DynamicUpdate
public abstract class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    protected Integer id;

    private String nom;
    private String prenom;

    @Column(unique = true, nullable = false)
    private String email;

    private String motDePasse;
    private String telephone;

    @Enumerated(EnumType.STRING)
    private StatutCompte statutCompte = StatutCompte.ACTIF;

    private int tentativesConnexion = 0;

    // A null value in an existing H2 row is version zero.
    private Integer sessionVersion;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "user_id")
    private List<Notification> notifications = new ArrayList<>();

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "owner_user_id")
    private List<MoyenPaiement> moyensPaiement = new ArrayList<>();

    protected User() {}

    public User(String nom, String prenom, String email, String motDePasse, String telephone) {
        this.nom = nom;
        this.prenom = prenom;
        this.email = email;
        this.motDePasse = motDePasse;
        this.telephone = telephone;
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }
    public String getPrenom() { return prenom; }
    public void setPrenom(String prenom) { this.prenom = prenom; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    @JsonIgnore
    public String getMotDePasse() { return motDePasse; }
    public void setMotDePasse(String motDePasse) { this.motDePasse = motDePasse; }
    public String getTelephone() { return telephone; }
    public void setTelephone(String telephone) { this.telephone = telephone; }
    public StatutCompte getStatutCompte() { return statutCompte; }
    public void setStatutCompte(StatutCompte statutCompte) { this.statutCompte = statutCompte; }
    public int getTentativesConnexion() { return tentativesConnexion; }
    public void setTentativesConnexion(int tentativesConnexion) { this.tentativesConnexion = tentativesConnexion; }

    public int getSessionVersion() { return sessionVersion == null ? 0 : sessionVersion; }

    public void suspendreCompte() {
        this.statutCompte = StatutCompte.SUSPENDU;
        this.sessionVersion = getSessionVersion() + 1;
    }

    public void bloquerCompte() {
        this.statutCompte = StatutCompte.BLOQUE;
        this.sessionVersion = getSessionVersion() + 1;
    }

    public void activerCompte() {
        this.statutCompte = StatutCompte.ACTIF;
        enregistrerConnexionReussie();
    }

    public void enregistrerEchecConnexion() {
        this.tentativesConnexion++;
        if (this.tentativesConnexion >= 3) bloquerCompte();
    }

    public void enregistrerConnexionReussie() {
        this.tentativesConnexion = 0;
    }

    public void ajouterNotification(Notification notification) {
        if (notification != null) this.notifications.add(notification);
    }

    public List<Notification> consulterNotifications() { return new ArrayList<>(notifications); }

    public void ajouterMoyenPaiement(MoyenPaiement moyenPaiement) {
        if (moyenPaiement != null) this.moyensPaiement.add(moyenPaiement);
    }

    public List<MoyenPaiement> getMoyensPaiement() { return new ArrayList<>(moyensPaiement); }

    @Override
    public String toString() {
        return "User{id=" + id + ", nom='" + nom + "', prenom='" + prenom + "', email='" + email + "', statutCompte=" + statutCompte + "}";
    }

    @Override
    public boolean equals(Object o) {
        return this == o || (o instanceof User user && Objects.equals(id, user.id));
    }

    @Override
    public int hashCode() { return Objects.hash(id); }
}

package com.exa.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

/**
 * Représente un utilisateur de l'application.
 */
@Entity
@Table(name = "users")
public class User {

    @Id
    @Column(length = 10, nullable = false)
    private String matricule;

    @Column(length = 30, nullable = false)
    private String nom;

    @Column(length = 50, nullable = false)
    private String prenom;

    @Column(length = 254, nullable = false, unique = true)
    private String email;

    @JsonIgnore
    @Column(length = 60, nullable = false)
    private String motDePasse;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Column(nullable = false)
    private boolean status = true;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Column(nullable = false)
    private LocalDateTime dateInscription = LocalDateTime.now();

    @JsonIgnore
    @OneToMany(mappedBy = "envoyeur", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Message> messagesEnvoyes = new ArrayList<>();

    @JsonIgnore
    @OneToMany(mappedBy = "receveur", cascade = CascadeType.ALL)
    private List<Message> messagesRecus = new ArrayList<>();

    /** Constructeur vide requis par JPA. */
    public User() {
    }

    /** Constructeur complet destiné à faciliter la création d'un utilisateur. */
    public User(String matricule, String nom, String prenom, String email,
                String motDePasse, Role role) {
        this.matricule = matricule;
        this.nom = nom;
        this.prenom = prenom;
        this.email = email;
        this.motDePasse = motDePasse;
        this.role = role;
    }

    public String getMatricule() {
        return matricule;
    }

    public void setMatricule(String matricule) {
        this.matricule = matricule;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public String getPrenom() {
        return prenom;
    }

    public void setPrenom(String prenom) {
        this.prenom = prenom;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getMotDePasse() {
        return motDePasse;
    }

    public void setMotDePasse(String motDePasse) {
        this.motDePasse = motDePasse;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public boolean isStatus() {
        return status;
    }

    public void setStatus(boolean status) {
        this.status = status;
    }

    public LocalDateTime getDateInscription() {
        return dateInscription;
    }

    public void setDateInscription(LocalDateTime dateInscription) {
        this.dateInscription = dateInscription;
    }

    public List<Message> getMessagesEnvoyes() {
        return messagesEnvoyes;
    }

    public void setMessagesEnvoyes(List<Message> messagesEnvoyes) {
        this.messagesEnvoyes = messagesEnvoyes;
    }

    public List<Message> getMessagesRecus() {
        return messagesRecus;
    }

    public void setMessagesRecus(List<Message> messagesRecus) {
        this.messagesRecus = messagesRecus;
    }

    /** Rôles disponibles dans l'application. */
    public enum Role {
        ETUDIANT,
        PROF,
        ADMIN
    }
}

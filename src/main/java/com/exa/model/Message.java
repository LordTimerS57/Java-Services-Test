package com.exa.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

/**
 * Représente un message public ou privé.
 */
@Entity
@Table(name = "messages")
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(length = 200, nullable = false)
    private String objet;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String contenu;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Column(nullable = false)
    private LocalDateTime dateDePublication = LocalDateTime.now();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Statut statut = Statut.PUBLIE;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "envoyeur_matricule", nullable = false)
    @JsonIgnoreProperties({ "messagesEnvoyes", "messagesRecus", "motDePasse" })
    private User envoyeur;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "receveur_matricule", nullable = true)
    @JsonIgnoreProperties({ "messagesEnvoyes", "messagesRecus", "motDePasse" })
    private User receveur;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "message_parent_id", nullable = true)
    @JsonIgnoreProperties({ "messagesReponses", "envoyeur", "receveur" })
    private Message messageParent;

    // EAGER + tri chronologique : l'auteur (envoyeur) de chaque réponse est
    // maintenant sérialisé (on ne masque plus que "messageParent", pour éviter
    // la boucle infinie parent <-> enfant en JSON).
    @OneToMany(mappedBy = "messageParent", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("dateDePublication ASC")
    @JsonIgnoreProperties({ "messageParent" })
    private List<Message> messagesReponses = new ArrayList<>();

    /** Constructeur vide requis par JPA. */
    public Message() {
    }

    /** Construit un message public. */
    public Message(User envoyeur, String objet, String contenu) {
        this.envoyeur = envoyeur;
        this.objet = objet;
        this.contenu = contenu;
    }

    /** Construit un message privé. */
    public Message(User envoyeur, User receveur, String objet, String contenu) {
        this.envoyeur = envoyeur;
        this.receveur = receveur;
        this.objet = objet;
        this.contenu = contenu;
    }

    /** Indique si le message est public, c'est-à-dire sans destinataire. */
    @Transient
    public boolean isPublic() {
        return receveur == null;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getObjet() {
        return objet;
    }

    public void setObjet(String objet) {
        this.objet = objet;
    }

    public String getContenu() {
        return contenu;
    }

    public void setContenu(String contenu) {
        this.contenu = contenu;
    }

    public LocalDateTime getDateDePublication() {
        return dateDePublication;
    }

    public void setDateDePublication(LocalDateTime dateDePublication) {
        this.dateDePublication = dateDePublication;
    }

    public Statut getStatut() {
        return statut;
    }

    public void setStatut(Statut statut) {
        this.statut = statut;
    }

    public User getEnvoyeur() {
        return envoyeur;
    }

    public void setEnvoyeur(User envoyeur) {
        this.envoyeur = envoyeur;
    }

    public User getReceveur() {
        return receveur;
    }

    public void setReceveur(User receveur) {
        this.receveur = receveur;
    }

    public Message getMessageParent() {
        return messageParent;
    }

    public void setMessageParent(Message messageParent) {
        this.messageParent = messageParent;
    }

    public List<Message> getMessagesReponses() {
        return messagesReponses;
    }

    public void setMessagesReponses(List<Message> messagesReponses) {
        this.messagesReponses = messagesReponses;
    }

    /** Statuts possibles pour un message. */
    public enum Statut {
        PUBLIE,
        SIGNALE,
        MASQUE,
        SUPPRIME
    }
}
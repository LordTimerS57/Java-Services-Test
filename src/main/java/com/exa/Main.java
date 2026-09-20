package com.exa;

import java.util.List;

import com.exa.model.Message;
import com.exa.model.User;
import com.exa.model.User.Role;
import com.exa.util.PasswordUtil;   // remplacer par com.exa.util.PasswordUtil si vous avez changé son package
import com.exa.util.JPAUtil;

import jakarta.persistence.EntityManager;

/**
 * Point d'entrée simple pour peupler la base (données de démonstration).
 * Rejouable : les utilisateurs et messages existants sont mis à jour, pas dupliqués.
 */
public class Main {

    public static void main(String[] args) {
        EntityManager em = null;

        try {
            em = JPAUtil.getEntityManager();
            em.getTransaction().begin();

            // ------------------------------------------------------------
            // 1. Utilisateurs (status = true : non bloqué ; connecte : état de connexion)
            // ------------------------------------------------------------

            // Professeur - mot de passe : Prof2026!
            User prof = upsert(em, "PROF001", "Randria", "Ainaridia",
                    "ainaridia3@gmail.com", "Prof2026!", Role.PROF, false);

            // Étudiant - mot de passe : Etudiant2026!
            User etudiant = upsert(em, "9045", "Rakotoarivelo", "Ainarala",
                    "ainarala3@gmail.com", "Etudiant2026!", Role.ETUDIANT, true);

            // Administrateur - mot de passe : Admin2026!
            upsert(em, "ADM001", "Andry", "Rado",
                    "admin@univ.mg", "Admin2026!", Role.ADMIN, false);

            // ------------------------------------------------------------
            // 2. Messages
            // ------------------------------------------------------------

            // Message public du professeur : bienvenue
            seedMessage(em, prof,
                    "Bienvenue - Analyse 2",
                    "Bienvenue dans le cours d'analyse 2 ! Nous aborderons les intégrales doubles, "
                            + "les changements de variables et les applications géométriques.",
                    null);

            // Question de l'étudiant
            Message question = seedMessage(em, etudiant,
                    "Question ? Analyse 2 - Intégrale Double changement de variable",
                    "Bonjour Monsieur, je ne comprends pas bien la méthode du changement de variable "
                            + "pour les intégrales doubles. Pouvez-vous donner un exemple concret ?",
                    null);

            // Réponse du professeur liée à la question de l'étudiant
            seedMessage(em, prof,
                    "Réponse - Changement de variable",
                    "Bonjour " + etudiant.getPrenom() + ", le changement de variable consiste à transformer "
                            + "le domaine et la fonction à intégrer via une application bijective. "
                            + "Exemple : passer en coordonnées polaires pour un disque.",
                    question);

            em.getTransaction().commit();
            System.out.println("Utilisateurs et messages enregistrés avec succès.");

            // ------------------------------------------------------------
            // 3. Lecture avec JPQL
            // ------------------------------------------------------------
            List<User> users = em.createQuery("SELECT u FROM User u ORDER BY u.matricule", User.class).getResultList();
            System.out.println("\n===== UTILISATEURS =====\n");
            for (User u : users) {
                System.out.println(u.getPrenom() + " " + u.getNom() + " <" + u.getEmail() + "> ("
                        + u.getRole() + ") - "
                        + (u.isConnecte() ? "connecté" : "non connecté") + " - "
                        + (u.isStatus() ? "non bloqué" : "bloqué"));
            }

            List<Message> messages = em.createQuery("SELECT m FROM Message m", Message.class).getResultList();
            System.out.println("\n===== LISTE DES MESSAGES =====\n");
            for (Message currentMessage : messages) {
                System.out.println("Objet    : " + currentMessage.getObjet());
                System.out.println("Contenu  : " + currentMessage.getContenu());
                System.out.println("Envoyeur : " + currentMessage.getEnvoyeur().getPrenom()
                        + " " + currentMessage.getEnvoyeur().getNom()
                        + " (" + currentMessage.getEnvoyeur().getRole() + ")");
                if (currentMessage.getMessageParent() != null) {
                    System.out.println("En réponse à : " + currentMessage.getMessageParent().getObjet());
                }
                System.out.println("------------------------------");
            }

        } catch (Exception exception) {
            if (em != null && em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            System.err.println("Erreur lors de l'opération JPA : " + exception.getMessage());
            exception.printStackTrace();
        } finally {
            if (em != null && em.isOpen()) {
                em.close();
            }
            JPAUtil.close();
        }
    }

    /** Crée l'utilisateur ou met à jour son profil s'il existe déjà (compte non bloqué). */
    private static User upsert(EntityManager em, String matricule, String nom, String prenom,
                               String email, String motDePasse, Role role, boolean connecte) {
        User user = em.find(User.class, matricule);
        if (user == null) {
            user = new User(matricule, nom, prenom, email, PasswordUtil.hash(motDePasse), role);
            em.persist(user);
        } else {
            user.setNom(nom);
            user.setPrenom(prenom);
            user.setEmail(email);
            user.setMotDePasse(PasswordUtil.hash(motDePasse));
            user.setRole(role);
        }
        user.setStatus(true);         // non bloqué
        user.setConnecte(connecte);   // état de connexion
        return user;
    }

    /** Crée le message public s'il n'existe pas déjà pour cet envoyeur et cet objet, sinon met à jour son contenu. */
    private static Message seedMessage(EntityManager em, User envoyeur, String objet, String contenu, Message parent) {
        Message message = em.createQuery(
                "SELECT m FROM Message m WHERE m.envoyeur.matricule = :matricule AND m.objet = :objet", Message.class)
                .setParameter("matricule", envoyeur.getMatricule())
                .setParameter("objet", objet)
                .setMaxResults(1)
                .getResultStream().findFirst().orElse(null);
        if (message == null) {
            message = new Message(envoyeur, objet, contenu);
            message.setMessageParent(parent);
            em.persist(message);
        } else {
            message.setContenu(contenu);
        }
        return message;
    }
}
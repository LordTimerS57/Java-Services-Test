package com.exa;

import java.util.List;

import com.exa.model.Message;
import com.exa.model.User;
import com.exa.model.User.Role;
import com.exa.util.JPAUtil;

import jakarta.persistence.EntityManager;

/**
 * Point d'entrée simple pour tester la persistance JPA en mode Java Application.
 * Teste notamment le fil de discussion : Message -> Message (réponse) -> Nouveau Message.
 */
public class Main {

    public static void main(String[] args) {
        EntityManager em = null;

        try {
            em = JPAUtil.getEntityManager();
            em.getTransaction().begin();

            // ------------------------------------------------------------
            // 1. Création des utilisateurs
            // ------------------------------------------------------------

            User prof = new User("PROF001", "Rabe", "Koto", "rabe@univ.mg", "hashProf", Role.PROF);
            em.persist(prof);

            User etudiant = new User("9045", "Rasoa", "Miora", "miora@univ.mg", "hashEtu", Role.ETUDIANT);
            em.persist(etudiant);

            User admin = new User("ADM001", "Andry", "Rado", "admin@univ.mg", "hashAdmin", Role.ADMIN);
            em.persist(admin);

            // ------------------------------------------------------------
            // 2. Fil de discussion : Message -> Réponse -> Réponse à la réponse
            // ------------------------------------------------------------

            // Message racine du professeur : bienvenue (pas de parent)
            Message msgBienvenue = new Message(
                    prof,
                    "Bienvenue - Analyse 2",
                    "Bienvenue dans le cours d'analyse 2 ! Nous aborderons les intégrales doubles, "
                            + "les changements de variables et les applications géométriques."
            );
            em.persist(msgBienvenue);

            // Question de l'étudiant : nouveau message racine, indépendant
            Message msgQuestion = new Message(
                    etudiant,
                    "Question ? Analyse 2 - Intégrale Double changement de variable",
                    "Bonjour Monsieur, je ne comprends pas bien la méthode du changement de variable "
                            + "pour les intégrales doubles. Pouvez-vous donner un exemple concret ?"
            );
            em.persist(msgQuestion);

            // Réponse du professeur à la question (niveau 1)
            Message msgReponse = new Message(
                    prof,
                    "Réponse - Changement de variable",
                    "Bonjour Miora, le changement de variable consiste à transformer le domaine "
                            + "et la fonction à intégrer via une application bijective. "
                            + "Exemple : passer en coordonnées polaires pour un disque."
            );
            msgReponse.setMessageParent(msgQuestion);
            em.persist(msgReponse);

            // Relance de l'étudiant en réponse à la réponse du professeur (niveau 2)
            Message msgRelance = new Message(
                    etudiant,
                    "Re: Réponse - Changement de variable",
                    "Merci Monsieur ! Et pour un domaine elliptique, on utiliserait "
                            + "les coordonnées polaires généralisées, c'est bien ça ?"
            );
            msgRelance.setMessageParent(msgReponse);
            em.persist(msgRelance);

            // Confirmation du professeur en réponse à la relance (niveau 3)
            Message msgConfirmation = new Message(
                    prof,
                    "Re: Re: Réponse - Changement de variable",
                    "Exactement, avec x = a*r*cos(θ) et y = b*r*sin(θ), "
                            + "et un jacobien égal à a*b*r."
            );
            msgConfirmation.setMessageParent(msgRelance);
            em.persist(msgConfirmation);

            // Nouveau message indépendant de l'étudiant (aucun rapport avec le fil précédent)
            Message msgNouveau = new Message(
                    etudiant,
                    "Question - Théorème de Fubini",
                    "Bonjour, pourriez-vous préciser les conditions d'application "
                            + "du théorème de Fubini pour les intégrales doubles ?"
            );
            em.persist(msgNouveau);

            em.getTransaction().commit();
            System.out.println("Utilisateurs et messages persistés avec succès.");

            // ------------------------------------------------------------
            // 3. Lecture des messages RACINES uniquement (comme le fera l'API)
            //    puis affichage récursif de chaque fil de discussion
            // ------------------------------------------------------------
            List<Message> racines = em.createQuery(
                    "SELECT m FROM Message m WHERE m.receveur IS NULL AND m.messageParent IS NULL "
                            + "ORDER BY m.dateDePublication ASC",
                    Message.class
            ).getResultList();

            System.out.println("\n===== ARBORESCENCE DES MESSAGES =====\n");
            for (Message racine : racines) {
                afficherFil(racine, 0);
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

    /**
     * Affiche récursivement un message et toutes ses réponses imbriquées,
     * avec une indentation croissante selon la profondeur.
     */
    private static void afficherFil(Message message, int profondeur) {
        String indent = "  ".repeat(profondeur);
        System.out.println(indent + "Objet    : " + message.getObjet());
        System.out.println(indent + "Contenu  : " + message.getContenu());
        System.out.println(indent + "Envoyeur : " + message.getEnvoyeur().getPrenom()
                + " " + message.getEnvoyeur().getNom()
                + " (" + message.getEnvoyeur().getRole() + ")");

        for (Message reponse : message.getMessagesReponses()) {
            System.out.println(indent + "  ↳ Réponse :");
            afficherFil(reponse, profondeur + 1);
        }
    }
}
package com.exa;

import java.util.List;

import com.exa.model.Message;
import com.exa.model.User;
import com.exa.model.User.Role;
import com.exa.util.JPAUtil;

import jakarta.persistence.EntityManager;

/**
 * Point d'entrée simple pour tester la persistance JPA en mode Java Application.
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

            // Professeur
            User prof = new User(
                    "PROF001",
                    "Rabe",
                    "Koto",
                    "rabe@univ.mg",
                    "hashProf",
                    Role.PROF
            );
            em.persist(prof);

            // Étudiant (Matricule 9045)
            User etudiant = new User(
                    "9045",
                    "Rasoa",
                    "Miora",
                    "miora@univ.mg",
                    "hashEtu",
                    Role.ETUDIANT
            );
            em.persist(etudiant);

            // Administrateur (optionnel, pour illustrer le rôle ADMIN)
            User admin = new User(
                    "ADM001",
                    "Andry",
                    "Rado",
                    "admin@univ.mg",
                    "hashAdmin",
                    Role.ADMIN
            );
            em.persist(admin);

            // ------------------------------------------------------------
            // 2. Création des messages
            // ------------------------------------------------------------

            // Message public du professeur : bienvenue
            Message msgBienvenue = new Message(
                    prof,
                    "Bienvenue - Analyse 2",
                    "Bienvenue dans le cours d'analyse 2 ! Nous aborderons les intégrales doubles, "
                            + "les changements de variables et les applications géométriques."
            );
            em.persist(msgBienvenue);

            // Question de l'étudiant
            Message msgQuestion = new Message(
                    etudiant,
                    "Question ? Analyse 2 - Intégrale Double changement de variable",
                    "Bonjour Monsieur, je ne comprends pas bien la méthode du changement de variable "
                            + "pour les intégrales doubles. Pouvez-vous donner un exemple concret ?"
            );
            em.persist(msgQuestion);

            // Réponse du professeur liée à la question de l'étudiant
            Message msgReponse = new Message(
                    prof,
                    "Réponse - Changement de variable",
                    "Bonjour Miora, le changement de variable consiste à transformer le domaine "
                            + "et la fonction à intégrer via une application bijective. "
                            + "Exemple : passer en coordonnées polaires pour un disque."
            );
            msgReponse.setMessageParent(msgQuestion);
            em.persist(msgReponse);

            em.getTransaction().commit();
            System.out.println("Utilisateurs et messages persistés avec succès.");

            // ------------------------------------------------------------
            // 3. Lecture de tous les messages avec JPQL
            // ------------------------------------------------------------
            List<Message> messages = em.createQuery(
                    "SELECT m FROM Message m",
                    Message.class
            ).getResultList();

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
}
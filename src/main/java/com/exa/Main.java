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

            // Création et persistance d'un utilisateur professeur.
            User user = new User(
                    "PROF001",
                    "Rabe",
                    "Koto",
                    "rabe@univ.mg",
                    "hashTest",
                    Role.PROF
            );
            em.persist(user);

            // Création et persistance d'un message public.
            Message message = new Message(
                    user,
                    "Bienvenue",
                    "Bienvenue dans le cours Java avancée !"
            );
            em.persist(message);

            em.getTransaction().commit();
            System.out.println("Utilisateur et message persistés avec succès.");

            // Lecture de tous les messages avec une requête JPQL.
            List<Message> messages = em.createQuery(
                    "SELECT m FROM Message m",
                    Message.class
            ).getResultList();

            for (Message currentMessage : messages) {
                System.out.println("Objet : " + currentMessage.getObjet());
                System.out.println("Contenu : " + currentMessage.getContenu());
                System.out.println("Envoyeur : " + currentMessage.getEnvoyeur().getNom());
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

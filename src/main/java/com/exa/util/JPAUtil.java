package com.exa.util;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

/**
 * Fournit les EntityManager de l'application et gère la factory JPA.
 */
public final class JPAUtil {

    private static final EntityManagerFactory ENTITY_MANAGER_FACTORY;

    static {
        try {
            ENTITY_MANAGER_FACTORY = Persistence.createEntityManagerFactory("monProjetPU");
        } catch (Exception exception) {
            throw new ExceptionInInitializerError(exception);
        }
    }

    /** Constructeur privé : cette classe utilitaire ne doit pas être instanciée. */
    private JPAUtil() {
    }

    /** Crée un EntityManager indépendant pour une unité de travail. */
    public static EntityManager getEntityManager() {
        return ENTITY_MANAGER_FACTORY.createEntityManager();
    }

    /** Ferme la factory JPA à l'arrêt de l'application. */
    public static void close() {
        if (ENTITY_MANAGER_FACTORY.isOpen()) {
            ENTITY_MANAGER_FACTORY.close();
        }
    }
}

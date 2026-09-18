package com.exa.rest;

import java.util.List;

import com.exa.model.Message;
import com.exa.model.User;
import com.exa.util.JPAUtil;

import jakarta.persistence.EntityManager;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/** Ressource REST pour la gestion des utilisateurs. */
@Path("/users")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class UserResource {

    @GET
    public Response getAll() {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            List<User> users = em.createQuery("SELECT u FROM User u", User.class).getResultList();
            return Response.ok(users).build();
        } finally {
            em.close();
        }
    }

    @GET
    @Path("/{matricule}")
    public Response getByMatricule(@PathParam("matricule") String matricule) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            User user = em.find(User.class, matricule);
            return user == null ? Response.status(Response.Status.NOT_FOUND).build() : Response.ok(user).build();
        } finally {
            em.close();
        }
    }

    @POST
    public Response create(User user) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            em.persist(user);
            em.getTransaction().commit();
            return Response.status(Response.Status.CREATED).entity(user).build();
        } catch (RuntimeException exception) {
            rollback(em);
            return Response.serverError().build();
        } finally {
            em.close();
        }
    }

    @PUT
    @Path("/{matricule}")
    public Response update(@PathParam("matricule") String matricule, User received) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            User user = em.find(User.class, matricule);
            if (user == null) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }
            em.getTransaction().begin();
            user.setNom(received.getNom());
            user.setPrenom(received.getPrenom());
            user.setEmail(received.getEmail());
            user.setMotDePasse(received.getMotDePasse());
            user.setRole(received.getRole());
            user.setStatus(received.isStatus());
            em.getTransaction().commit();
            return Response.ok(user).build();
        } catch (RuntimeException exception) {
            rollback(em);
            return Response.serverError().build();
        } finally {
            em.close();
        }
    }

    @DELETE
    @Path("/{matricule}")
    public Response delete(@PathParam("matricule") String matricule) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            User user = em.find(User.class, matricule);
            if (user == null) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }
            em.getTransaction().begin();
            em.remove(user);
            em.getTransaction().commit();
            return Response.noContent().build();
        } catch (RuntimeException exception) {
            rollback(em);
            return Response.serverError().build();
        } finally {
            em.close();
        }
    }

    @GET
    @Path("/{matricule}/messages")
    public Response getMessages(@PathParam("matricule") String matricule) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            User user = em.find(User.class, matricule);
            if (user == null) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }
            List<Message> messages = em.createQuery(
                    "SELECT m FROM Message m WHERE m.envoyeur.matricule = :matricule", Message.class)
                    .setParameter("matricule", matricule)
                    .getResultList();
            return Response.ok(messages).build();
        } finally {
            em.close();
        }
    }

    private void rollback(EntityManager em) {
        if (em.getTransaction().isActive()) {
            em.getTransaction().rollback();
        }
    }
}

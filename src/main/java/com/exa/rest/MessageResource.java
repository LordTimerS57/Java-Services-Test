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

/** Ressource REST pour la gestion des messages. */
@Path("/messages")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class MessageResource {

    @GET
    public Response getPublicMessages() {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            List<Message> messages = em.createQuery(
                    "SELECT m FROM Message m WHERE m.receveur IS NULL", Message.class).getResultList();
            return Response.ok(messages).build();
        } finally {
            em.close();
        }
    }

    @GET
    @Path("/{id}")
    public Response getById(@PathParam("id") int id) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            Message message = em.find(Message.class, id);
            return message == null ? Response.status(Response.Status.NOT_FOUND).build() : Response.ok(message).build();
        } finally {
            em.close();
        }
    }

    @POST
    public Response create(Message received) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            User sender = findUser(em, received.getEnvoyeur());
            if (sender == null) {
                return Response.status(Response.Status.BAD_REQUEST).build();
            }
            User receiver = received.getReceveur() == null ? null : findUser(em, received.getReceveur());
            if (received.getReceveur() != null && receiver == null) {
                return Response.status(Response.Status.BAD_REQUEST).build();
            }
            received.setEnvoyeur(sender);
            received.setReceveur(receiver);
            em.getTransaction().begin();
            em.persist(received);
            em.getTransaction().commit();
            return Response.status(Response.Status.CREATED).entity(received).build();
        } catch (RuntimeException exception) {
            rollback(em);
            return Response.serverError().build();
        } finally {
            em.close();
        }
    }

    @PUT
    @Path("/{id}")
    public Response update(@PathParam("id") int id, Message received) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            Message message = em.find(Message.class, id);
            if (message == null) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }
            em.getTransaction().begin();
            message.setObjet(received.getObjet());
            message.setContenu(received.getContenu());
            if (received.getStatut() != null) {
                message.setStatut(received.getStatut());
            }
            em.getTransaction().commit();
            return Response.ok(message).build();
        } catch (RuntimeException exception) {
            rollback(em);
            return Response.serverError().build();
        } finally {
            em.close();
        }
    }

    @DELETE
    @Path("/{id}")
    public Response delete(@PathParam("id") int id) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            Message message = em.find(Message.class, id);
            if (message == null) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }
            em.getTransaction().begin();
            em.remove(message);
            em.getTransaction().commit();
            return Response.noContent().build();
        } catch (RuntimeException exception) {
            rollback(em);
            return Response.serverError().build();
        } finally {
            em.close();
        }
    }

    @PUT
    @Path("/{id}/signaler")
    public Response signal(@PathParam("id") int id) {
        return changeStatus(id, Message.Statut.SIGNALE);
    }

    @PUT
    @Path("/{id}/masquer")
    public Response hide(@PathParam("id") int id) {
        return changeStatus(id, Message.Statut.MASQUE);
    }

    private Response changeStatus(int id, Message.Statut status) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            Message message = em.find(Message.class, id);
            if (message == null) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }
            em.getTransaction().begin();
            message.setStatut(status);
            em.getTransaction().commit();
            return Response.ok(message).build();
        } catch (RuntimeException exception) {
            rollback(em);
            return Response.serverError().build();
        } finally {
            em.close();
        }
    }

    private User findUser(EntityManager em, User user) {
        return user == null || user.getMatricule() == null
                ? null
                : em.find(User.class, user.getMatricule());
    }

    private void rollback(EntityManager em) {
        if (em.getTransaction().isActive()) {
            em.getTransaction().rollback();
        }
    }
}

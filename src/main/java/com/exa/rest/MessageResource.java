package com.exa.rest;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.exa.model.Message;
import com.exa.model.User;
import com.exa.rest.dto.PopularSubject;
import com.exa.util.JPAUtil;

import jakarta.persistence.EntityManager;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/** REST resource for reading, filtering and managing public messages. */
@Path("/messages")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class MessageResource {
    @GET
    public Response getPublicMessages(@QueryParam("q") String query,
                                      @QueryParam("objet") String subject,
                                      @QueryParam("from") String from,
                                      @QueryParam("to") String to,
                                      @DefaultValue("date") @QueryParam("sort") String sort) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            LocalDateTime start = parseDate(from, false);
            LocalDateTime end = parseDate(to, true);
            List<Message> messages = em.createQuery("SELECT m FROM Message m WHERE m.receveur IS NULL", Message.class).getResultList()
                    .stream().filter(m -> matches(m, query, subject, start, end))
                    .sorted((a, b) -> "oldest".equalsIgnoreCase(sort)
                            ? a.getDateDePublication().compareTo(b.getDateDePublication())
                            : b.getDateDePublication().compareTo(a.getDateDePublication()))
                    .collect(Collectors.toList());
            return Response.ok(messages).build();
        } catch (IllegalArgumentException exception) {
            return bad("Format de date attendu : yyyy-MM-dd");
        } finally { em.close(); }
    }

    /** Returns recurring subjects, ordered by number of publications. */
    @GET
    @Path("/popular")
    public Response getPopularSubjects(@QueryParam("q") String query,
                                       @QueryParam("from") String from,
                                       @QueryParam("to") String to,
                                       @DefaultValue("10") @QueryParam("limit") int limit) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            LocalDateTime start = parseDate(from, false);
            LocalDateTime end = parseDate(to, true);
            Map<String, List<Message>> grouped = em.createQuery("SELECT m FROM Message m WHERE m.receveur IS NULL", Message.class).getResultList()
                    .stream().filter(m -> matches(m, query, null, start, end))
                    .collect(Collectors.groupingBy(m -> m.getObjet().trim(), LinkedHashMap::new, Collectors.toList()));
            int safeLimit = Math.max(1, Math.min(limit, 100));
            List<PopularSubject> popular = grouped.entrySet().stream()
                    .map(entry -> new PopularSubject(entry.getKey(), entry.getValue().size(),
                            entry.getValue().stream().map(Message::getDateDePublication).max(Comparator.naturalOrder()).toString()))
                    .sorted(Comparator.comparingLong((PopularSubject item) -> item.occurrences).reversed()
                            .thenComparing(item -> item.objet, String.CASE_INSENSITIVE_ORDER))
                    .limit(safeLimit).collect(Collectors.toList());
            return Response.ok(popular).build();
        } catch (IllegalArgumentException exception) {
            return bad("Format de date attendu : yyyy-MM-dd");
        } finally { em.close(); }
    }

    @GET @Path("/{id}")
    public Response getById(@PathParam("id") int id) {
        EntityManager em = JPAUtil.getEntityManager();
        try { Message message = em.find(Message.class, id); return message == null ? Response.status(Response.Status.NOT_FOUND).build() : Response.ok(message).build(); }
        finally { em.close(); }
    }

    @POST
    public Response create(Message received) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            if (received == null || blank(received.getObjet()) || blank(received.getContenu())) return bad("Objet et contenu sont requis");
            User sender = findUser(em, received.getEnvoyeur());
            if (sender == null) return bad("Envoyeur introuvable");
            User receiver = received.getReceveur() == null ? null : findUser(em, received.getReceveur());
            if (received.getReceveur() != null && receiver == null) return bad("Destinataire introuvable");
            received.setEnvoyeur(sender); received.setReceveur(receiver);
            em.getTransaction().begin(); em.persist(received); em.getTransaction().commit();
            return Response.status(Response.Status.CREATED).entity(received).build();
        } catch (RuntimeException exception) { rollback(em); return Response.serverError().build(); } finally { em.close(); }
    }

    @PUT @Path("/{id}")
    public Response update(@PathParam("id") int id, Message received) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            Message message = em.find(Message.class, id); if (message == null) return Response.status(Response.Status.NOT_FOUND).build();
            em.getTransaction().begin(); message.setObjet(received.getObjet()); message.setContenu(received.getContenu());
            if (received.getStatut() != null) message.setStatut(received.getStatut()); em.getTransaction().commit(); return Response.ok(message).build();
        } catch (RuntimeException exception) { rollback(em); return Response.serverError().build(); } finally { em.close(); }
    }

    @DELETE @Path("/{id}")
    public Response delete(@PathParam("id") int id) {
        EntityManager em = JPAUtil.getEntityManager();
        try { Message message = em.find(Message.class, id); if (message == null) return Response.status(Response.Status.NOT_FOUND).build(); em.getTransaction().begin(); em.remove(message); em.getTransaction().commit(); return Response.noContent().build(); }
        catch (RuntimeException exception) { rollback(em); return Response.serverError().build(); } finally { em.close(); }
    }

    @PUT @Path("/{id}/signaler") public Response signal(@PathParam("id") int id) { return changeStatus(id, Message.Statut.SIGNALE); }
    @PUT @Path("/{id}/masquer") public Response hide(@PathParam("id") int id) { return changeStatus(id, Message.Statut.MASQUE); }

    private Response changeStatus(int id, Message.Statut status) {
        EntityManager em = JPAUtil.getEntityManager();
        try { Message message = em.find(Message.class, id); if (message == null) return Response.status(Response.Status.NOT_FOUND).build(); em.getTransaction().begin(); message.setStatut(status); em.getTransaction().commit(); return Response.ok(message).build(); }
        catch (RuntimeException exception) { rollback(em); return Response.serverError().build(); } finally { em.close(); }
    }
    private boolean matches(Message m, String query, String subject, LocalDateTime start, LocalDateTime end) {
        String q = query == null ? "" : query.trim().toLowerCase(); String s = subject == null ? "" : subject.trim().toLowerCase();
        String objet = m.getObjet() == null ? "" : m.getObjet().toLowerCase(); String contenu = m.getContenu() == null ? "" : m.getContenu().toLowerCase();
        LocalDateTime date = m.getDateDePublication(); return (q.isBlank() || objet.contains(q) || contenu.contains(q)) && (s.isBlank() || objet.contains(s)) && (start == null || !date.isBefore(start)) && (end == null || !date.isAfter(end));
    }
    private LocalDateTime parseDate(String value, boolean end) { if (value == null || value.isBlank()) return null; return end ? LocalDate.parse(value).atTime(LocalTime.MAX) : LocalDate.parse(value).atStartOfDay(); }
    private User findUser(EntityManager em, User user) { return user == null || user.getMatricule() == null ? null : em.find(User.class, user.getMatricule()); }
    private boolean blank(String value) { return value == null || value.isBlank(); }
    private Response bad(String message) { return Response.status(Response.Status.BAD_REQUEST).entity("{\"message\":\"" + message + "\"}").build(); }
    private void rollback(EntityManager em) { if (em.getTransaction().isActive()) em.getTransaction().rollback(); }
}

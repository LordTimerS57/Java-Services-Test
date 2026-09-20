package com.exa.rest;

import java.util.Map;

import com.exa.model.User;
import com.exa.rest.dto.EmailUpdateRequest;
import com.exa.rest.dto.PasswordUpdateRequest;
import com.exa.rest.dto.ProfileUpdateRequest;
import com.exa.util.JPAUtil;
import com.exa.util.MailUtil;
import com.exa.util.PasswordResetStore;
import com.exa.util.PasswordUtil;

import jakarta.persistence.EntityManager;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/** Separate endpoints for personal information, email and password changes. */
@Path("/users")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class UserAccountResource {
    @PUT
    @Path("/{matricule}/profile")
    public Response updateProfile(@PathParam("matricule") String matricule, ProfileUpdateRequest request) {
        if (request == null || blank(request.currentPassword)) return bad("Mot de passe actuel requis");
        EntityManager em = JPAUtil.getEntityManager();
        try {
            User user = findAuthorizedUser(em, matricule, request.currentPassword);
            if (user == null) return unauthorizedOrNotFound(em, matricule);
            em.getTransaction().begin();
            if (request.nom != null && !request.nom.isBlank()) user.setNom(request.nom.trim());
            if (request.prenom != null && !request.prenom.isBlank()) user.setPrenom(request.prenom.trim());
            em.getTransaction().commit();
            return Response.ok(user).build();
        } catch (RuntimeException exception) { rollback(em); return serverError(exception); } finally { em.close(); }
    }

    @PUT
    @Path("/{matricule}/email")
    public Response updateEmail(@PathParam("matricule") String matricule, EmailUpdateRequest request) {
        if (request == null || blank(request.currentPassword) || !validEmail(request.email)) return bad("Email et mot de passe actuel requis");
        EntityManager em = JPAUtil.getEntityManager();
        try {
            User user = findAuthorizedUser(em, matricule, request.currentPassword);
            if (user == null) return unauthorizedOrNotFound(em, matricule);
            String email = request.email.trim().toLowerCase();
            boolean used = !em.createQuery("SELECT u FROM User u WHERE LOWER(u.email) = :email AND u.matricule <> :matricule", User.class)
                    .setParameter("email", email).setParameter("matricule", matricule).setMaxResults(1).getResultList().isEmpty();
            if (used) return conflict("Email déjà utilisé");
            em.getTransaction().begin(); user.setEmail(email); em.getTransaction().commit();
            return Response.ok(user).build();
        } catch (RuntimeException exception) { rollback(em); return serverError(exception); } finally { em.close(); }
    }

    @POST
    @Path("/{matricule}/password/request")
    public Response requestPasswordChange(@PathParam("matricule") String matricule, PasswordUpdateRequest request) {
        if (request == null || !validPassword(request.newPassword)) return bad("Le nouveau mot de passe doit contenir au moins 8 caractères");
        EntityManager em = JPAUtil.getEntityManager();
        try {
            User user = em.find(User.class, matricule);
            if (user == null) return Response.status(Response.Status.NOT_FOUND).build();
            String code = PasswordResetStore.create(matricule, request.newPassword);
            if (code == null) return Response.status(429).entity("{\"message\":\"Un code vient d'être envoyé. Patientez une minute avant d'en redemander un.\"}").build();
            try {
                MailUtil.sendPasswordCode(user.getEmail(), user.getPrenom(), code);
            } catch (RuntimeException exception) {
                PasswordResetStore.cancel(matricule);
                return serverError(exception);
            }
            return Response.ok(Map.of("email", mask(user.getEmail()))).build();
        } finally { em.close(); }
    }

    @POST
    @Path("/{matricule}/password/confirm")
    public Response confirmPasswordChange(@PathParam("matricule") String matricule, PasswordUpdateRequest request) {
        if (request == null || blank(request.code)) return bad("Code requis");
        EntityManager em = JPAUtil.getEntityManager();
        try {
            User user = em.find(User.class, matricule);
            if (user == null) return Response.status(Response.Status.NOT_FOUND).build();
            PasswordResetStore.Outcome outcome = PasswordResetStore.verify(matricule, request.code.trim());
            switch (outcome.status) {
                case NONE:    return bad("Aucune demande en cours. Renvoyez un code.");
                case EXPIRED: return bad("Code expiré. Renvoyez un code.");
                case LOCKED:  return bad("Trop de tentatives. Renvoyez un code.");
                case INVALID: return bad("Code incorrect");
                default: break;
            }
            em.getTransaction().begin();
            user.setMotDePasse(outcome.passwordHash);
            em.getTransaction().commit();
            return Response.noContent().build();
        } catch (RuntimeException exception) { rollback(em); return serverError(exception); } finally { em.close(); }
    }

    private String mask(String email) {
        int at = email == null ? -1 : email.indexOf('@');
        return at <= 1 ? String.valueOf(email) : email.charAt(0) + "***" + email.substring(at);
    }

    private User findAuthorizedUser(EntityManager em, String matricule, String password) {
        User user = em.find(User.class, matricule);
        return user != null && PasswordUtil.matches(password, user.getMotDePasse()) ? user : null;
    }
    private Response unauthorizedOrNotFound(EntityManager em, String matricule) { return em.find(User.class, matricule) == null ? Response.status(Response.Status.NOT_FOUND).build() : unauthorized(); }
    private Response unauthorized() { return Response.status(Response.Status.UNAUTHORIZED).entity("{\"message\":\"Mot de passe incorrect\"}").build(); }
    private Response bad(String message) { return Response.status(Response.Status.BAD_REQUEST).entity("{\"message\":\"" + message + "\"}").build(); }
    private Response conflict(String message) { return Response.status(Response.Status.CONFLICT).entity("{\"message\":\"" + message + "\"}").build(); }
    private Response serverError(Exception exception) { exception.printStackTrace(); return Response.serverError().entity("{\"message\":\"Erreur interne du serveur\"}").build(); }
    private boolean blank(String value) { return value == null || value.isBlank(); }
    private boolean validEmail(String email) { return email != null && email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$"); }
    private boolean validPassword(String password) { return password != null && password.length() >= 8; }
    private void rollback(EntityManager em) { if (em.getTransaction().isActive()) em.getTransaction().rollback(); }
}

package com.exa.rest;

import com.exa.model.User;
import com.exa.rest.dto.EmailUpdateRequest;
import com.exa.rest.dto.PasswordUpdateRequest;
import com.exa.rest.dto.ProfileUpdateRequest;
import com.exa.util.JPAUtil;
import jakarta.persistence.EntityManager;
import jakarta.ws.rs.Consumes;
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

    @PUT
    @Path("/{matricule}/password")
    public Response updatePassword(@PathParam("matricule") String matricule, PasswordUpdateRequest request) {
    	if (request == null || !validPassword(request.newPassword)) return bad("Le nouveau mot de passe doit contenir au moins 8 caractères");
        EntityManager em = JPAUtil.getEntityManager();
        try {
            User user = em.find(User.class, matricule);
            if (user == null) return Response.status(Response.Status.NOT_FOUND).build();
            em.getTransaction().begin(); user.setMotDePasse(PasswordUtil.hash(request.newPassword)); em.getTransaction().commit();
            return Response.noContent().build();
        } catch (RuntimeException exception) { rollback(em); return serverError(exception); } finally { em.close(); }
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

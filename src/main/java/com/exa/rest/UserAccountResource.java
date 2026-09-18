package com.exa.rest;

import com.exa.model.User;
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

/** Authenticated profile and password changes. The current password authorizes each change. */
@Path("/users")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class UserAccountResource {
    @PUT
    @Path("/{matricule}/profile")
    public Response updateProfile(@PathParam("matricule") String matricule, ProfileUpdateRequest request) {
        if (request == null || request.currentPassword == null) return bad("Mot de passe actuel requis");
        EntityManager em = JPAUtil.getEntityManager();
        try {
            User user = em.find(User.class, matricule);
            if (user == null) return Response.status(Response.Status.NOT_FOUND).build();
            if (!PasswordUtil.matches(request.currentPassword, user.getMotDePasse())) return unauthorized();
            if (request.email != null && !request.email.equalsIgnoreCase(user.getEmail())) {
                if (!request.email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) return bad("Email invalide");
                boolean used = !em.createQuery("SELECT u FROM User u WHERE LOWER(u.email) = :email AND u.matricule <> :matricule", User.class)
                        .setParameter("email", request.email.trim().toLowerCase()).setParameter("matricule", matricule).setMaxResults(1).getResultList().isEmpty();
                if (used) return conflict("Email déjà utilisé");
                user.setEmail(request.email.trim().toLowerCase());
            }
            em.getTransaction().begin();
            if (request.nom != null && !request.nom.isBlank()) user.setNom(request.nom.trim());
            if (request.prenom != null && !request.prenom.isBlank()) user.setPrenom(request.prenom.trim());
            em.getTransaction().commit(); return Response.ok(user).build();
        } catch (RuntimeException exception) { rollback(em); return Response.serverError().build(); } finally { em.close(); }
    }

    @PUT
    @Path("/{matricule}/password")
    public Response updatePassword(@PathParam("matricule") String matricule, PasswordUpdateRequest request) {
        if (request == null || request.currentPassword == null || request.newPassword == null || request.newPassword.length() < 8) return bad("Mot de passe invalide");
        EntityManager em = JPAUtil.getEntityManager();
        try {
            User user = em.find(User.class, matricule);
            if (user == null) return Response.status(Response.Status.NOT_FOUND).build();
            if (!PasswordUtil.matches(request.currentPassword, user.getMotDePasse())) return unauthorized();
            em.getTransaction().begin(); user.setMotDePasse(PasswordUtil.hash(request.newPassword)); em.getTransaction().commit();
            return Response.noContent().build();
        } catch (RuntimeException exception) { rollback(em); return Response.serverError().build(); } finally { em.close(); }
    }
    private Response unauthorized() { return Response.status(Response.Status.UNAUTHORIZED).entity("{\"message\":\"Mot de passe incorrect\"}").build(); }
    private Response bad(String message) { return Response.status(Response.Status.BAD_REQUEST).entity("{\"message\":\"" + message + "\"}").build(); }
    private Response conflict(String message) { return Response.status(Response.Status.CONFLICT).entity("{\"message\":\"" + message + "\"}").build(); }
    private void rollback(EntityManager em) { if (em.getTransaction().isActive()) em.getTransaction().rollback(); }
}

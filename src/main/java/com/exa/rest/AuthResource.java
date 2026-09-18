package com.exa.rest;

import com.exa.model.User;
import com.exa.rest.dto.LoginRequest;
import com.exa.rest.dto.RegisterRequest;
import com.exa.util.JPAUtil;
import jakarta.persistence.EntityManager;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.UUID;

@Path("/auth")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AuthResource {
    @POST @Path("/register")
    public Response register(RegisterRequest request) {
        if (request == null || blank(request.matricule) || blank(request.nom) || blank(request.prenom) || !validEmail(request.email) || !validPassword(request.motDePasse)) return error(Response.Status.BAD_REQUEST, "Informations d'inscription invalides");
        EntityManager em = JPAUtil.getEntityManager();
        try {
            String email = request.email.trim().toLowerCase();
            if (em.find(User.class, request.matricule.trim()) != null || emailExists(em, email)) return error(Response.Status.CONFLICT, "Matricule ou email déjà utilisé");
            User user = new User(request.matricule.trim(), request.nom.trim(), request.prenom.trim(), email, PasswordUtil.hash(request.motDePasse), request.role == null ? User.Role.ETUDIANT : request.role);
            em.getTransaction().begin(); em.persist(user); em.getTransaction().commit();
            return Response.status(Response.Status.CREATED).entity(authResponse(user)).build();
        } catch (RuntimeException exception) { rollback(em); exception.printStackTrace(); return error(Response.Status.INTERNAL_SERVER_ERROR, "Inscription impossible"); } finally { em.close(); }
    }

    @POST @Path("/login")
    public Response login(LoginRequest request) {
        if (request == null || !validEmail(request.email) || blank(request.motDePasse)) return error(Response.Status.UNAUTHORIZED, "Email ou mot de passe incorrect");
        EntityManager em = JPAUtil.getEntityManager();
        try {
            User user = em.createQuery("SELECT u FROM User u WHERE LOWER(u.email) = :email", User.class).setParameter("email", request.email.trim().toLowerCase()).setMaxResults(1).getResultStream().findFirst().orElse(null);
            if (user == null || !user.isStatus() || !PasswordUtil.matches(request.motDePasse, user.getMotDePasse())) return error(Response.Status.UNAUTHORIZED, "Email ou mot de passe incorrect");
            if (!user.getMotDePasse().contains(":")) { em.getTransaction().begin(); user.setMotDePasse(PasswordUtil.hash(request.motDePasse)); em.getTransaction().commit(); }
            return Response.ok(authResponse(user)).build();
        } catch (RuntimeException exception) { exception.printStackTrace(); return error(Response.Status.INTERNAL_SERVER_ERROR, "Connexion impossible"); } finally { em.close(); }
    }
    private boolean emailExists(EntityManager em, String email) { return !em.createQuery("SELECT u FROM User u WHERE LOWER(u.email) = :email", User.class).setParameter("email", email).setMaxResults(1).getResultList().isEmpty(); }
    private Object authResponse(User user) { return new Object() { public final String token = UUID.randomUUID().toString(); public final User utilisateur = user; }; }
    private Response error(Response.Status status, String message) { return Response.status(status).entity("{\"message\":\"" + message + "\"}").build(); }
    private boolean blank(String value) { return value == null || value.isBlank(); }
    private boolean validEmail(String email) { return email != null && email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$"); }
    private boolean validPassword(String password) { return password != null && password.length() >= 8; }
    private void rollback(EntityManager em) { if (em.getTransaction().isActive()) em.getTransaction().rollback(); }
}

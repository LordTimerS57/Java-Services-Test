package com.exa.rest.dto;

/** Registration payload. Role is accepted as text so the API can normalize frontend values. */
public class RegisterRequest {
    public String matricule;
    public String nom;
    public String prenom;
    public String email;
    public String motDePasse;
    public String role;
}

package com.exa.rest.dto;

import com.exa.model.User;

public class RegisterRequest {
    public String matricule;
    public String nom;
    public String prenom;
    public String email;
    public String motDePasse;
    public User.Role role;
}

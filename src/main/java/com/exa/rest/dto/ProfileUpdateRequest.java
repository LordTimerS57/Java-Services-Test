package com.exa.rest.dto;

/** Authenticated profile update. Password is required to authorize sensitive changes. */
public class ProfileUpdateRequest {
    public String currentPassword;
    public String nom;
    public String prenom;
    public String email;
}

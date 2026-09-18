package com.exa.rest.dto;

/** Email update request, protected by the current password. */
public class EmailUpdateRequest {
    public String currentPassword;
    public String email;
}

package com.exa.rest;

import java.util.HashSet;
import java.util.Set;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;

/** Configuration explicite des ressources REST de l'application. */
@ApplicationPath("/api")
public class ApplicationConfig extends Application {

    @Override
    public Set<Class<?>> getClasses() {
        Set<Class<?>> classes = new HashSet<>();
        classes.add(UserResource.class);
        classes.add(MessageResource.class);
        return classes;
    }
}

package com.exa.rest;

import java.util.HashSet;
import java.util.Set;
import jakarta.ws.rs.core.Application;

public class ApplicationConfig extends Application {
    @Override
    public Set<Class<?>> getClasses() {
        Set<Class<?>> classes = new HashSet<>();
        classes.add(UserResource.class);
        classes.add(UserAccountResource.class);
        classes.add(AuthResource.class);
        classes.add(MessageResource.class);
        classes.add(JacksonConfig.class);
        return classes;
    }
}

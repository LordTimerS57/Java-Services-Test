package com.exa.rest;

import java.util.HashSet;
import java.util.Set;

import jakarta.ws.rs.core.Application;

/**
 * Classe d'application JAX-RS.
 *
 * On l'utilise principalement avec web.xml, qui pointe vers le package com.exa.rest.
 * L'annotation @ApplicationPath est volontairement omise ici pour éviter un doublon
 * de registration avec le mapping Jersey défini dans web.xml.
 */
public class ApplicationConfig extends Application {

    @Override
    public Set<Class<?>> getClasses() {
        Set<Class<?>> classes = new HashSet<>();
        classes.add(UserResource.class);
        classes.add(MessageResource.class);
        return classes;
    }
}

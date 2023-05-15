package de.ids_mannheim.korap.web.utils;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Defines the list of {@link javax.ws.rs.container.ContainerRequestFilter}
 * and {@link javax.ws.rs.container.ContainerResponseFilter}
 * classes associated with a resource method.
 * <p>
 * This annotation can be specified on a class or on method(s). Specifying it
 * at a class level means that it applies to all the methods in the class.
 * Specifying it on a method means that it is applicable to that method only.
 * If applied at both the class and methods level , the method value overrides
 * the class value.
 */
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
public @interface ResourceFilters {
    Class<?>[] value();
}

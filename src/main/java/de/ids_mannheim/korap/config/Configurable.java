package de.ids_mannheim.korap.config;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * @author hanl
 * @date 27/07/2015
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface Configurable {
    String value();
}

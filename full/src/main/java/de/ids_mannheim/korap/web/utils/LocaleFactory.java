package de.ids_mannheim.korap.web.utils;

import java.util.List;
import java.util.Locale;

import org.glassfish.hk2.api.Factory;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.ext.Provider;

@Provider
public class LocaleFactory implements Factory<Locale> {
    @Context
    private ContainerRequestContext context;

    @Override
    public Locale provide() {
        final List<Locale> locales = context.getAcceptableLanguages();
        if (locales.isEmpty())
            return Locale.US;
        return locales.get(0);
    }

    @Override
    public void dispose(Locale instance) {}
}

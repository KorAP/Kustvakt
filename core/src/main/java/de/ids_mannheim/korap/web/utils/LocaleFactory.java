package de.ids_mannheim.korap.web.utils;

import org.glassfish.hk2.api.Factory;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.ext.Provider;
import java.util.List;
import java.util.Locale;

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

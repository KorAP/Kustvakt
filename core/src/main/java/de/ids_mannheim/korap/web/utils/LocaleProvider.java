package de.ids_mannheim.korap.web.utils;

import com.sun.jersey.api.core.HttpContext;
import com.sun.jersey.core.spi.component.ComponentContext;
import com.sun.jersey.core.spi.component.ComponentScope;
import com.sun.jersey.server.impl.inject.AbstractHttpContextInjectable;
import com.sun.jersey.spi.inject.Injectable;
import com.sun.jersey.spi.inject.InjectableProvider;

import javax.ws.rs.core.Context;
import javax.ws.rs.ext.Provider;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Locale;

/**
 * @author hanl
 * @date 04/02/2014
 */
@Provider
public class LocaleProvider extends AbstractHttpContextInjectable<Locale>
        implements InjectableProvider<Context, Type> {

    @Override
    public Locale getValue (HttpContext httpContext) {
        final List<Locale> locales = httpContext.getRequest()
                .getAcceptableLanguages();
        if (locales.isEmpty())
            return Locale.US;
        return locales.get(0);
    }


    @Override
    public ComponentScope getScope () {
        return ComponentScope.PerRequest;
    }


    @Override
    public Injectable getInjectable (ComponentContext ic, Context context,
            Type type) {
        if (type.equals(Locale.class))
            return this;
        return null;
    }
}

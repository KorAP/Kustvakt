package de.ids_mannheim.korap.web.utils;

import java.net.URI;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sun.jersey.api.NotFoundException;

import de.ids_mannheim.korap.config.KustvaktConfiguration;

@Component
@Provider
public class NotFoundMapper implements ExceptionMapper<NotFoundException> {

    private static Logger jlog =
            LogManager.getLogger(NotFoundMapper.class);
    public static final Pattern VERSION_PATTERN =
            Pattern.compile("/(v[0-9][^/]*)(/.*)");
    private static final boolean DEBUG = false;

    @Autowired
    private KustvaktConfiguration config;

    @Override
    public Response toResponse (NotFoundException exception) {
        URI notFoundUri = exception.getNotFoundUri();

        String path = notFoundUri.getPath();
        String baseUrl = config.getBaseURL();
        baseUrl = baseUrl.substring(0, baseUrl.length() - 2);

        if (path.startsWith(baseUrl)) {
            path = path.substring(baseUrl.length(), path.length());
            Matcher matcher = VERSION_PATTERN.matcher(path);
            if (!matcher.matches()) {
                path = baseUrl + "/" + config.getCurrentVersion() + path;
                URI redirectUri = UriBuilder.fromUri(notFoundUri)
                        .replacePath(path).build();
                if (DEBUG){
                    jlog.debug("REDIRECT: "+redirectUri.toString());
                }
                return Response.status(HttpStatus.PERMANENT_REDIRECT_308)
                        .location(redirectUri).build();
            }
            else if (!matcher.group(1).equals(config.getCurrentVersion())){
                path = baseUrl + "/" + config.getCurrentVersion()
                        + matcher.group(2);
                URI redirectUri = UriBuilder.fromUri(notFoundUri)
                        .replacePath(path).build();
                if (DEBUG){
                    jlog.debug("REDIRECT replace: "+ redirectUri.toString());
                }
                return Response.status(HttpStatus.PERMANENT_REDIRECT_308)
                        .location(redirectUri).build();
            }
        }
        return Response.status(HttpStatus.NOT_FOUND_404).build();
    }
}

package de.ids_mannheim.korap.utils;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.MediaType;

import org.glassfish.jersey.message.internal.MediaTypes;
import org.glassfish.jersey.server.ContainerRequest;

public class JerseyUtils {

    /**
     * Get the form parameters of the request entity.
     * <p>
     * This method will ensure that the request entity is buffered
     * such that it may be consumed by the application.
     *
     * @return the form parameters, if there is a request entity and the
     * content type is "application/x-www-form-urlencoded", otherwise an
     * instance containing no parameters will be returned.
     */
    public static Form getFormParameters (ContainerRequestContext requestContext) {
        if (requestContext instanceof ContainerRequest) {
            return getFormParameters((ContainerRequest) requestContext);
        }
        return new Form();
    }

    private static Form getFormParameters (ContainerRequest request) {
        if (MediaTypes.typeEqual(MediaType.APPLICATION_FORM_URLENCODED_TYPE, request.getMediaType())) {
            request.bufferEntity();
            Form form = request.readEntity(Form.class);
            return (form == null ? new Form() : form);
        } else {
            return new Form();
        }
    }
}

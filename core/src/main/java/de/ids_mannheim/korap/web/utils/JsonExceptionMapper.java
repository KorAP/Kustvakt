package de.ids_mannheim.korap.web.utils;

import javax.annotation.Priority;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import com.fasterxml.jackson.databind.JsonMappingException;

import de.ids_mannheim.korap.exceptions.StatusCodes;
import de.ids_mannheim.korap.web.CoreResponseHandler;

/**
 * Creates appropriate responses in case of incorrect JSON
 * deserialization or JsonMappingException, for instance when a
 * request parameter should be deserialized as an ENUM but the
 * parameter value does not match any of the available ENUM values.
 * 
 * @author margaretha
 *
 */
@Priority(value = 1)
@Provider
public class JsonExceptionMapper
        implements ExceptionMapper<JsonMappingException> {

    @Override
    public Response toResponse (JsonMappingException exception) {
        String entity = CoreResponseHandler.buildNotification(
                StatusCodes.DESERIALIZATION_FAILED, exception.getMessage(),
                null);
        return Response.status(Response.Status.BAD_REQUEST).entity(entity)
                .build();
    }

}

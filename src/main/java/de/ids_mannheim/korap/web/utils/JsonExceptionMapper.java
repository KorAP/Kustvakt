package de.ids_mannheim.korap.web.utils;

import com.fasterxml.jackson.databind.JsonMappingException;

import de.ids_mannheim.korap.exceptions.StatusCodes;
import de.ids_mannheim.korap.web.CoreResponseHandler;
import jakarta.annotation.Priority;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

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

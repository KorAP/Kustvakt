package de.ids_mannheim.korap.exceptions;

import lombok.Data;

/**
 * @author hanl
 * @date 11/12/2013
 */
// a security table registers all these exceptions (failed authentication, failed access to a resource, etc.)
@Data
@Deprecated
public class NotAuthorizedException extends BaseException {

    public NotAuthorizedException(int status) {
        super(status);
    }

    public NotAuthorizedException(int status, String entity) {
        super(status, "", entity);
    }

    public NotAuthorizedException(int status, String message, String entity) {
        super(status, message, entity);
    }

    public NotAuthorizedException(Throwable cause, int status) {
        super(cause, status);
    }

    public NotAuthorizedException(String message, Throwable cause, int status) {
        super(message, cause, status);
    }
}

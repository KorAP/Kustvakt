package de.ids_mannheim.korap.exceptions;

/**
 * @author hanl
 * @date 25/03/2014
 */
@Deprecated // even useful anymore?
public class EmptyResultException extends KustvaktException {

    public EmptyResultException(String message, String entity) {
        super(StatusCodes.EMPTY_RESULTS, message, entity);
    }

    public EmptyResultException(String entity) {
        super(StatusCodes.EMPTY_RESULTS, "", entity);
    }

}

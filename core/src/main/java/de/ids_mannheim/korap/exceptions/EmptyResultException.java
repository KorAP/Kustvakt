package de.ids_mannheim.korap.exceptions;

/**
 * @author hanl
 * @date 25/03/2014
 */
@Deprecated
// even useful anymore?
public class EmptyResultException extends KustvaktException {

    public EmptyResultException (String message, String entity) {
        super(StatusCodes.NO_RESULT_FOUND, message, entity);
    }


    public EmptyResultException (String entity) {
        super(StatusCodes.NO_RESULT_FOUND, "No entity found for id", entity);
    }

}

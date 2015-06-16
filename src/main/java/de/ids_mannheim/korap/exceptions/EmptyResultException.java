package de.ids_mannheim.korap.exceptions;

/**
 * @author hanl
 * @date 25/03/2014
 */
public class EmptyResultException extends BaseException {

    public EmptyResultException(String entity) {
        super(StatusCodes.EMPTY_RESULTS, entity);
    }

}

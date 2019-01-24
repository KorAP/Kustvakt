package de.ids_mannheim.korap.validator;

import java.util.Map;

import org.springframework.stereotype.Component;

import de.ids_mannheim.korap.exceptions.KustvaktException;

/**
 * EM: made this as a spring component
 * 
 * Created by hanl on 08.06.16.
 */
@Component
public interface Validator {


    Map<String, Object> validateMap (Map<String, Object> map) throws KustvaktException;


    String validateEntry (String input, String type)
            throws KustvaktException;


    boolean isValid (String input, String type);
}

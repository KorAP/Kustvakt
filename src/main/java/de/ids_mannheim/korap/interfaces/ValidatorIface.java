package de.ids_mannheim.korap.interfaces;

import de.ids_mannheim.korap.exceptions.KustvaktException;

import java.util.Map;

/**
 * Created by hanl on 08.06.16.
 */
public interface ValidatorIface {


    Map<String, Object> validateMap (Map<String, Object> map) throws KustvaktException;


    String validateEntry (String input, String type)
            throws KustvaktException;


    boolean isValid (String input, String type);
}

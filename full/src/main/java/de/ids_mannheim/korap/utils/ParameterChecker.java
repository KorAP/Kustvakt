package de.ids_mannheim.korap.utils;

import java.util.Collection;

import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.exceptions.StatusCodes;

public class ParameterChecker {

    public static void checkObjectValue (Object obj, String name)
            throws KustvaktException {
        if (obj == null) {
            throw new KustvaktException(StatusCodes.INVALID_ARGUMENT,
                    name + " is null", name);
        }
    }

    public static void checkCollection (Collection<?> collection, String name)
            throws KustvaktException {
        if (collection == null) {
            throw new KustvaktException(StatusCodes.INVALID_ARGUMENT,
                    name + " is null", name);
        }
        else if (collection.isEmpty()) {
            throw new KustvaktException(StatusCodes.INVALID_ARGUMENT, 
                    name + " is empty", name);
        }
    }

    public static void checkStringValue (String string, String name)
            throws KustvaktException {
        if (string == null) {
            throw new KustvaktException(StatusCodes.INVALID_ARGUMENT, 
                    name + " is null", name);
        }
        else if (string.isEmpty()) {
            throw new KustvaktException(StatusCodes.INVALID_ARGUMENT, 
                    name + " is empty", name);
        }
    }

    public static void checkIntegerValue (int integer, String name)
            throws KustvaktException {
        if (integer == 0) {
            throw new KustvaktException(StatusCodes.MISSING_PARAMETER, 
                    name + " is missing", name);
        }
    }
    
    public static void checkNameValue (String value, String name)
            throws KustvaktException {
        if (value == null) {
            throw new KustvaktException(StatusCodes.INVALID_ARGUMENT,
                    name + " is null", name);
        }
        else if (value.length() < 3) {
            throw new KustvaktException(StatusCodes.INVALID_ARGUMENT,
                    name+" must contain at least 3 characters", name);
        }
    }
}

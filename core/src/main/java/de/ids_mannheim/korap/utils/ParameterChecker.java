package de.ids_mannheim.korap.utils;

import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.exceptions.StatusCodes;

public class ParameterChecker {

    public static void checkObjectValue (Object obj, String name)
            throws KustvaktException {
        if (obj == null) {
            throw new KustvaktException(StatusCodes.INVALID_ARGUMENT, name,
                    "null");
        }
    }
    
    public static void checkStringValue (String string, String name)
            throws KustvaktException {
        if (string == null || string.isEmpty()) {
            throw new KustvaktException(StatusCodes.INVALID_ARGUMENT, name,
                    string);
        }
    }

    public static void checkIntegerValue (int integer, String name) throws KustvaktException {
        if (integer == 0) {
            throw new KustvaktException(StatusCodes.INVALID_ARGUMENT, name,
                    "0");
        }
    }

}

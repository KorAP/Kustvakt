package de.ids_mannheim.korap.utils;

/**
 * @author hanl
 * @date 19/02/2014
 */
public class BooleanUtils {

    public static String dbname;

    public static Object getBoolean(Object val) {
        if (val == null)
            val = false;
        if (dbname != null && dbname.equalsIgnoreCase("sqlite")) {
            if (val instanceof Boolean) {
                return ((boolean) val) ? 1 : 0;
            }else if (val instanceof Integer) {
                return ((Integer) val == 1);
            }
        }
        return val;
    }
}

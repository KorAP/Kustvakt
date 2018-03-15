package de.ids_mannheim.korap.config;

import org.apache.commons.lang.WordUtils;

/** Lists possible authentication schemes used in the Authorization header 
 *  of HTTP requests.  
 * 
 * @author margaretha
 *
 */
public enum AuthenticationScheme {
    // standard http
    BASIC, BEARER,
    // custom
    SESSION, API;

    public String displayName () {
        return WordUtils.capitalizeFully(name());
    }
}

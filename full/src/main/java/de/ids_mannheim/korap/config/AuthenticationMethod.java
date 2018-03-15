package de.ids_mannheim.korap.config;

/** Lists possible actual authentication methods. Multiple 
 *  {@link AuthenticationScheme} may use an identical 
 *  authentication method. 
 * 
 * @author margaretha
 * 
 * @see AuthenticationScheme 
 *
 */
public enum AuthenticationMethod {
    LDAP, SHIBBOLETH, DATABASE; 
}

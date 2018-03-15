package de.ids_mannheim.korap.authentication.http;

import de.ids_mannheim.korap.config.AuthenticationScheme;
import lombok.Getter;
import lombok.Setter;

/** Describes the values stored in Authorization header of HTTP requests. 
 * 
 * @author margaretha
 *
 */
@Getter
@Setter
public class AuthorizationData {

    private String token;
    private AuthenticationScheme authenticationScheme;
    private String username;
    private String password;

}


package de.ids_mannheim.korap.web.input;

import de.ids_mannheim.korap.oauth2.constant.OAuth2ClientType;
import lombok.Getter;
import lombok.Setter;

/** Defines required attributes to register an OAuth2 client. 
 * 
 * @author margaretha
 *
 */
@Setter
@Getter
public class OAuth2ClientJson {

    // all required for registration
    private String name;
    private OAuth2ClientType type;
    private String url;
    // redirect URI determines where the OAuth 2.0 service will return the user to 
    // after they have authorized a client.
    private String redirectURI;
    private String description;
}

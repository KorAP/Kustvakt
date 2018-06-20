package de.ids_mannheim.korap.oauth2.oltu;

import javax.servlet.http.HttpServletRequest;

import org.apache.oltu.oauth2.as.request.OAuthAuthzRequest;
import org.apache.oltu.oauth2.common.OAuth;
import org.apache.oltu.oauth2.common.exception.OAuthProblemException;
import org.apache.oltu.oauth2.common.exception.OAuthSystemException;

/**
 * Customization of {@link OAuthAuthzRequest} from Apache Oltu.
 * Limit extraction of client id from request's parameters since
 * Kustvakt requires user authentication via Basic authentication for
 * authorization code requests.
 * 
 * @author margaretha
 *
 */
public class OAuth2AuthorizationRequest extends OAuthAuthzRequest {

    public OAuth2AuthorizationRequest (HttpServletRequest request)
            throws OAuthSystemException, OAuthProblemException {
        super(request);
    }

    @Override
    public String getClientId () {
        return getParam(OAuth.OAUTH_CLIENT_ID);
    }
}

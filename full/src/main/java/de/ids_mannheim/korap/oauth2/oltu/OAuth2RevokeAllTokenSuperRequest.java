package de.ids_mannheim.korap.oauth2.oltu;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.oltu.oauth2.as.request.OAuthRequest;
import org.apache.oltu.oauth2.common.OAuth;
import org.apache.oltu.oauth2.common.exception.OAuthProblemException;
import org.apache.oltu.oauth2.common.exception.OAuthSystemException;
import org.apache.oltu.oauth2.common.utils.OAuthUtils;
import org.apache.oltu.oauth2.common.validators.OAuthValidator;

/**
 * A custom request based on {@link OAuthRequest}. It defines a
 * request to revoke all tokens of a client. The request must have
 * been sent from a super client.
 * 
 * @author margaretha
 *
 */
public class OAuth2RevokeAllTokenSuperRequest {
    protected HttpServletRequest request;
    protected OAuthValidator<HttpServletRequest> validator;
    protected Map<String, Class<? extends OAuthValidator<HttpServletRequest>>> validators =
            new HashMap<String, Class<? extends OAuthValidator<HttpServletRequest>>>();

    public OAuth2RevokeAllTokenSuperRequest () {
        // TODO Auto-generated constructor stub
    }

    public OAuth2RevokeAllTokenSuperRequest (HttpServletRequest request)
            throws OAuthSystemException, OAuthProblemException {
        this.request = request;
        validate();
    }

    protected void validate ()
            throws OAuthSystemException, OAuthProblemException {
        validator = initValidator();
        validator.validateMethod(request);
        validator.validateContentType(request);
        validator.validateRequiredParameters(request);
        // for super client authentication
        validator.validateClientAuthenticationCredentials(request);
    }

    protected OAuthValidator<HttpServletRequest> initValidator ()
            throws OAuthProblemException, OAuthSystemException {
        return OAuthUtils.instantiateClass(RevokeAllTokenSuperValidator.class);
    }

    public String getParam (String name) {
        return request.getParameter(name);
    }

    public String getClientId () {
        return request.getParameter(OAuth.OAUTH_CLIENT_ID);
    }

    public String getSuperClientId () {
        return request.getParameter(RevokeTokenSuperValidator.SUPER_CLIENT_ID);
    }

    public String getSuperClientSecret () {
        return request
                .getParameter(RevokeTokenSuperValidator.SUPER_CLIENT_SECRET);
    }
}

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
 * A custom request based on {@link OAuthRequest}.
 * 
 * This class does not extend {@link OAuthRequest} because it contains some
 * parameters i.e. redirect_uri and scopes that are not parts of
 * revoke token request.
 * 
 * @author margaretha
 *
 */
public class OAuth2RevokeTokenRequest {

    protected HttpServletRequest request;
    protected OAuthValidator<HttpServletRequest> validator;
    protected Map<String, Class<? extends OAuthValidator<HttpServletRequest>>> validators =
            new HashMap<String, Class<? extends OAuthValidator<HttpServletRequest>>>();

    public OAuth2RevokeTokenRequest () {}

    public OAuth2RevokeTokenRequest (HttpServletRequest request)
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
//        validator.validateClientAuthenticationCredentials(request);
    }

    protected OAuthValidator<HttpServletRequest> initValidator ()
            throws OAuthProblemException, OAuthSystemException {
        return OAuthUtils.instantiateClass(RevokeTokenValidator.class);
    }

    public String getParam (String name) {
        return request.getParameter(name);
    }

    public String getToken () {
        return getParam("token");
    }
    
    public String getTokenType () {
        return getParam(OAuth.OAUTH_TOKEN_TYPE);
    }

    public String getClientId () {
        String[] creds = OAuthUtils.decodeClientAuthenticationHeader(
                request.getHeader(OAuth.HeaderType.AUTHORIZATION));
        if (creds != null) {
            return creds[0];
        }
        return getParam(OAuth.OAUTH_CLIENT_ID);
    }

    public String getClientSecret () {
        String[] creds = OAuthUtils.decodeClientAuthenticationHeader(
                request.getHeader(OAuth.HeaderType.AUTHORIZATION));
        if (creds != null) {
            return creds[1];
        }
        return getParam(OAuth.OAUTH_CLIENT_SECRET);
    }
}

package de.ids_mannheim.korap.oauth2.oltu;

import javax.servlet.http.HttpServletRequest;

import org.apache.oltu.oauth2.as.request.OAuthAuthzRequest;
import org.apache.oltu.oauth2.as.validator.CodeValidator;
import org.apache.oltu.oauth2.common.OAuth;
import org.apache.oltu.oauth2.common.error.OAuthError;
import org.apache.oltu.oauth2.common.exception.OAuthProblemException;
import org.apache.oltu.oauth2.common.exception.OAuthSystemException;
import org.apache.oltu.oauth2.common.message.types.ResponseType;
import org.apache.oltu.oauth2.common.utils.OAuthUtils;
import org.apache.oltu.oauth2.common.validators.OAuthValidator;

/**
 * Customization of {@link OAuthAuthzRequest} from Apache Oltu.
 * <ul>
 * <li>Limit extraction of client id from request's parameters since
 * Kustvakt requires user authentication via Basic authentication for
 * authorization code requests. </li>
 * 
 * <li>Exclude TokenValidator since it is not supported in
 * Kustvakt.</li>
 * 
 * <li>Minimize {{@link #validate()} to include missing response type
 * response in client redirect URI when the client id and redirect URI 
 * are valid. </li>
 * 
 * </ul>
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

    @Override
    protected OAuthValidator<HttpServletRequest> initValidator ()
            throws OAuthProblemException, OAuthSystemException {
        validators.put(ResponseType.CODE.toString(), CodeValidator.class);
        // validators.put(ResponseType.TOKEN.toString(),
        // TokenValidator.class);
        final String requestTypeValue = getParam(OAuth.OAUTH_RESPONSE_TYPE);
        if (requestTypeValue!=null && !requestTypeValue.isEmpty()) {
            if (requestTypeValue.equals(ResponseType.CODE.toString())) {
                
            }
            else if (requestTypeValue.equals(ResponseType.TOKEN.toString())) {
                throw OAuthProblemException.error(
                        OAuthError.CodeResponse.UNSUPPORTED_RESPONSE_TYPE)
                        .description("response_type token is not supported");
            }
            else {
                throw OAuthUtils.handleOAuthProblemException(
                        "Invalid response_type parameter value");
            }
        }
        
        return OAuthUtils.instantiateClass(validators.get("code"));
    }

    @Override
    protected void validate ()
            throws OAuthSystemException, OAuthProblemException {
        validator = initValidator();
        validator.validateMethod(request);
        validator.validateContentType(request);
        validator.validateRequiredParameters(request);
        validator.validateClientAuthenticationCredentials(request);
    }
}

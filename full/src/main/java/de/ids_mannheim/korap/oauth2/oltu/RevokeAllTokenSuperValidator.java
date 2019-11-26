package de.ids_mannheim.korap.oauth2.oltu;

import java.util.HashSet;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.apache.oltu.oauth2.common.OAuth;
import org.apache.oltu.oauth2.common.exception.OAuthProblemException;
import org.apache.oltu.oauth2.common.utils.OAuthUtils;
import org.apache.oltu.oauth2.common.validators.AbstractValidator;

import de.ids_mannheim.korap.oauth2.constant.OAuth2Error;

/**
 * Defines required request parameters for
 * OAuth2RevokeAllTokenSuperRequest and validates the request method.
 * 
 * @author margaretha
 *
 */
public class RevokeAllTokenSuperValidator
        extends AbstractValidator<HttpServletRequest> {

    public static final String SUPER_CLIENT_ID = "super_client_id";
    public static final String SUPER_CLIENT_SECRET = "super_client_secret";

    public RevokeAllTokenSuperValidator () {
        requiredParams.add(OAuth.OAUTH_CLIENT_ID);
        requiredParams.add(SUPER_CLIENT_ID);
        requiredParams.add(SUPER_CLIENT_SECRET);

        enforceClientAuthentication = true;
    }

    @Override
    public void validateMethod (HttpServletRequest request)
            throws OAuthProblemException {
        String method = request.getMethod();
        if (!OAuth.HttpMethod.POST.equals(method)) {
            throw OAuthProblemException.error(OAuth2Error.INVALID_REQUEST)
                    .description("Method not correct.");
        }
    }

    @Override
    public void validateClientAuthenticationCredentials (
            HttpServletRequest request) throws OAuthProblemException {
        if (enforceClientAuthentication) {
            Set<String> missingParameters = new HashSet<String>();

            if (OAuthUtils.isEmpty(request.getParameter(SUPER_CLIENT_ID))) {
                missingParameters.add(SUPER_CLIENT_ID);
            }
            if (OAuthUtils.isEmpty(request.getParameter(SUPER_CLIENT_SECRET))) {
                missingParameters.add(SUPER_CLIENT_SECRET);
            }

            if (!missingParameters.isEmpty()) {
                throw OAuthUtils.handleMissingParameters(missingParameters);
            }
        }
    }
}

package de.ids_mannheim.korap.oauth2.oltu;

import javax.servlet.http.HttpServletRequest;

import org.apache.oltu.oauth2.common.OAuth;
import org.apache.oltu.oauth2.common.exception.OAuthProblemException;
import org.apache.oltu.oauth2.common.validators.AbstractValidator;

import de.ids_mannheim.korap.oauth2.constant.OAuth2Error;

/**
 * A custom revoke token validator based on RFC 7009.
 * 
 * Additional changes to the RFC:
 * <ul>
 * <li>client_id is made required for public client
 * authentication</li>
 * </ul>
 * 
 * @author margaretha
 *
 */
public class RevokeTokenValidator
        extends AbstractValidator<HttpServletRequest> {

    public RevokeTokenValidator () {
        requiredParams.add("token");
        requiredParams.add(OAuth.OAUTH_CLIENT_ID);
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
    public void validateContentType (HttpServletRequest request)
            throws OAuthProblemException {}

}

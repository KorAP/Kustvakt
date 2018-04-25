package de.ids_mannheim.korap.oauth2;

import javax.servlet.http.HttpServletRequest;

import org.apache.oltu.oauth2.common.OAuth;
import org.apache.oltu.oauth2.common.exception.OAuthProblemException;
import org.apache.oltu.oauth2.common.utils.OAuthUtils;
import org.apache.oltu.oauth2.common.validators.AbstractValidator;

public class ClientDeregistrationValidator extends AbstractValidator<HttpServletRequest>{

    public ClientDeregistrationValidator () {
        enforceClientAuthentication = true;
    }
    
    @Override
    public void validateMethod (HttpServletRequest request)
            throws OAuthProblemException {
        if (!request.getMethod().equals(OAuth.HttpMethod.DELETE)) {
            throw OAuthUtils.handleOAuthProblemException("Method not set to DELETE.");
        }
    }
}

package de.ids_mannheim.korap.oauth;

import javax.servlet.http.HttpServletRequest;

import org.apache.oltu.oauth2.as.request.OAuthRequest;
import org.apache.oltu.oauth2.common.exception.OAuthProblemException;
import org.apache.oltu.oauth2.common.exception.OAuthSystemException;
import org.apache.oltu.oauth2.common.utils.OAuthUtils;
import org.apache.oltu.oauth2.common.validators.OAuthValidator;

public class OAuthDeregisterClientRequest extends OAuthRequest {

    public OAuthDeregisterClientRequest (HttpServletRequest request)
            throws OAuthSystemException, OAuthProblemException {
        super(request);
    }

    @Override
    protected OAuthValidator<HttpServletRequest> initValidator ()
            throws OAuthProblemException, OAuthSystemException {
        validators.put("client_deregistration",
                ClientDeregistrationValidator.class);
        final Class<? extends OAuthValidator<HttpServletRequest>> clazz =
                validators.get("client_deregistration");
        return OAuthUtils.instantiateClass(clazz);
    }
}

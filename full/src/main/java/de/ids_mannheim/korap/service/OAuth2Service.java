package de.ids_mannheim.korap.service;

import org.apache.oltu.oauth2.common.message.types.GrantType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import de.ids_mannheim.korap.entity.OAuth2Client;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.exceptions.StatusCodes;

@Service
public class OAuth2Service {

    @Autowired
    private OAuth2ClientService clientService;

    /** 
     *  RFC 6749:
     *  
     *  If the client type is confidential or the client was issued client
     *  credentials, the client MUST authenticate with the authorization server.
     *  
     * @param authorization
     * @param grantType
     * @param scope 
     * @param password 
     * @param username 
     * @param client_id 
     * @param redirectURI 
     * @param authorizationCode 
     * @throws KustvaktException
     */
    public void requestAccessToken (String authorization, GrantType grantType,
            String authorizationCode, String redirectURI, String client_id,
            String username, String password, String scope)
            throws KustvaktException {

        OAuth2Client client = clientService.authenticateClient(authorization,
                grantType, client_id);

        if (grantType.equals(GrantType.AUTHORIZATION_CODE)) {

        }
        else if (grantType.equals(GrantType.PASSWORD)) {

        }
        else if (grantType.equals(GrantType.CLIENT_CREDENTIALS)) {

        }
        else {
            throw new KustvaktException(StatusCodes.UNSUPPORTED_GRANT_TYPE,
                    "Grant type " + grantType.name() + " is unsupported.",
                    grantType.name());
        }

    }
}

package de.ids_mannheim.korap.authentication.http;

import org.springframework.stereotype.Component;

import de.ids_mannheim.korap.constant.AuthenticationScheme;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.exceptions.StatusCodes;
import de.ids_mannheim.korap.utils.ParameterChecker;

/**
 * Implementation of Basic HTTP authentication scheme (see RFC 7253
 * and 7617) for client asking for authorization and sending user
 * data.
 * 
 * @author margaretha
 * 
 */
@Component
public class HttpAuthorizationHandler {

    public static String createBasicAuthorizationHeaderValue (String username,
            String password) throws KustvaktException {
        ParameterChecker.checkStringValue(username, "username");
        ParameterChecker.checkStringValue(password, "password");

        String credentials = TransferEncoding.encodeBase64(username, password);
        return AuthenticationScheme.BASIC.displayName() + " " + credentials;
    }

    public AuthorizationData parseAuthorizationHeaderValue (
            String authorizationHeader) throws KustvaktException {
        ParameterChecker.checkStringValue(authorizationHeader,
                "authorization header");

        String[] values = authorizationHeader.split(" ");
        if (values.length != 2) {
            throw new KustvaktException(StatusCodes.AUTHENTICATION_FAILED,
                    "Cannot parse authorization header value "
                            + authorizationHeader
                            + ". Use this format: [authentication "
                            + "scheme] [Base64-encoded token]",
                    authorizationHeader);
        }

        AuthorizationData data = new AuthorizationData();
        String scheme = values[0];
        try {
            data.setAuthenticationScheme(
                    AuthenticationScheme.valueOf(scheme.toUpperCase()));
        }
        catch (IllegalArgumentException e) {
            throw new KustvaktException(StatusCodes.AUTHENTICATION_FAILED,
                    "Authentication scheme is not supported.", scheme);
        }
        data.setToken(values[1]);
        return data;
    }

    public AuthorizationData parseBasicToken (AuthorizationData data)
            throws KustvaktException {
        String[] credentials = TransferEncoding.decodeBase64(data.getToken());
        data.setUsername(credentials[0]);
        data.setPassword(credentials[1]);
        return data;
    }
}

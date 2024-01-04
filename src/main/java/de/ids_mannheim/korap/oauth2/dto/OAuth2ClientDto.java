package de.ids_mannheim.korap.oauth2.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 * Describes the client_id and the client_secret of a client after
 * client registration or reset secret.
 * 
 * @author margaretha
 *
 */
@JsonInclude(Include.NON_EMPTY)
public class OAuth2ClientDto {

    private String client_id;
    private String client_secret;

    public OAuth2ClientDto () {}

    public OAuth2ClientDto (String id, String secret) {
        this.setClient_id(id);
        this.setClient_secret(secret);
    }

    public String getClient_id () {
        return client_id;
    }

    public void setClient_id (String client_id) {
        this.client_id = client_id;
    }

    public String getClient_secret () {
        return client_secret;
    }

    public void setClient_secret (String client_secret) {
        this.client_secret = client_secret;
    }
}
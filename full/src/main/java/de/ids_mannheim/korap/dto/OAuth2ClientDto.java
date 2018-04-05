package de.ids_mannheim.korap.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OAuth2ClientDto {

    private String client_id;
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private String client_secret;

    public OAuth2ClientDto () {}

    public OAuth2ClientDto (String id, String secret) {
        this.client_id = id;
        this.client_secret = secret;
    }
}
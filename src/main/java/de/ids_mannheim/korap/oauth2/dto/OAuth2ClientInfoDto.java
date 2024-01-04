package de.ids_mannheim.korap.oauth2.dto;

import java.time.ZonedDateTime;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.oauth2.constant.OAuth2ClientType;
import de.ids_mannheim.korap.oauth2.entity.OAuth2Client;
import de.ids_mannheim.korap.utils.JsonUtils;

/**
 * Describes information about an OAuth2 client.
 * 
 * @author margaretha
 *
 */
@JsonInclude(Include.NON_EMPTY)
public class OAuth2ClientInfoDto {
    @JsonProperty("super")
    private boolean isSuper;

    @JsonProperty("client_id")
    private String clientId;
    @JsonProperty("client_name")
    private String clientName;
    @JsonProperty("client_type")
    private OAuth2ClientType clientType;
    @JsonProperty("client_description")
    private String description;
    @JsonProperty("client_url")
    private String url;
    @JsonProperty("client_redirect_uri")
    private String redirect_uri;
    @JsonProperty("registration_date")
    private String registrationDate;
    @JsonProperty("registered_by")
    private String registeredBy;
    @JsonProperty("refresh_token_expiry")
    private int refreshTokenExpiry; // in seconds

    @JsonProperty("permitted")
    private boolean isPermitted;
    private JsonNode source;

    public OAuth2ClientInfoDto (OAuth2Client client) throws KustvaktException {
        this.setClientId(client.getId());
        this.setClientName(client.getName());
        this.setDescription(client.getDescription());
        this.setClientType(client.getType());
        this.setUrl(client.getUrl());
        this.setClientType(client.getType());
        this.setRedirect_uri(client.getRedirectURI());
        this.setSuper(client.isSuper());
        this.setPermitted(client.isPermitted());
        this.setRegisteredBy(client.getRegisteredBy());

        String source = client.getSource();
        if (source != null && !source.isEmpty()) {
            this.source = JsonUtils.readTree(source);
        }
        if (client.getType().equals(OAuth2ClientType.CONFIDENTIAL)) {
            this.setRefreshTokenExpiry(client.getRefreshTokenExpiry());
        }
        ZonedDateTime registrationDate = client.getRegistrationDate();
        if (registrationDate != null) {
            this.setRegistrationDate(registrationDate.toString());
        }
    }

    public boolean isSuper () {
        return isSuper;
    }

    public void setSuper (boolean isSuper) {
        this.isSuper = isSuper;
    }

    public String getRegisteredBy () {
        return registeredBy;
    }

    public void setRegisteredBy (String registeredBy) {
        this.registeredBy = registeredBy;
    }

    public String getClientId () {
        return clientId;
    }

    public void setClientId (String clientId) {
        this.clientId = clientId;
    }

    public String getClientName () {
        return clientName;
    }

    public void setClientName (String clientName) {
        this.clientName = clientName;
    }

    public OAuth2ClientType getClientType () {
        return clientType;
    }

    public void setClientType (OAuth2ClientType clientType) {
        this.clientType = clientType;
    }

    public String getDescription () {
        return description;
    }

    public void setDescription (String description) {
        this.description = description;
    }

    public String getUrl () {
        return url;
    }

    public void setUrl (String url) {
        this.url = url;
    }

    public String getRedirect_uri () {
        return redirect_uri;
    }

    public void setRedirect_uri (String redirect_uri) {
        this.redirect_uri = redirect_uri;
    }

    public String getRegistrationDate () {
        return registrationDate;
    }

    public void setRegistrationDate (String registrationDate) {
        this.registrationDate = registrationDate;
    }

    public int getRefreshTokenExpiry () {
        return refreshTokenExpiry;
    }

    public void setRefreshTokenExpiry (int refreshTokenExpiry) {
        this.refreshTokenExpiry = refreshTokenExpiry;
    }

    public boolean isPermitted () {
        return isPermitted;
    }

    public void setPermitted (boolean isPermitted) {
        this.isPermitted = isPermitted;
    }

    public JsonNode getSource () {
        return source;
    }

    public void setSource (JsonNode source) {
        this.source = source;
    }
}

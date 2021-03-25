package de.ids_mannheim.korap.oauth2.dto;

import java.util.Set;

/**
 * Describes OAuth2 refresh tokens
 * 
 * @author margaretha
 *
 */
public class OAuth2TokenDto {

    private String token;
    private String createdDate;
    private String expiryDate;
    private String userAuthenticationTime;
    private Set<String> scopes;

    private String clientId;
    private String clientName;
    private String clientDescription;
    private String clientUrl;

    public String getToken () {
        return token;
    }

    public void setToken (String token) {
        this.token = token;
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

    public String getClientDescription () {
        return clientDescription;
    }

    public void setClientDescription (String clientDescription) {
        this.clientDescription = clientDescription;
    }

    public String getClientUrl () {
        return clientUrl;
    }

    public void setClientUrl (String clientUrl) {
        this.clientUrl = clientUrl;
    }

    public String getCreatedDate () {
        return createdDate;
    }

    public void setCreatedDate (String createdDate) {
        this.createdDate = createdDate;
    }

    public String getExpiryDate () {
        return expiryDate;
    }

    public void setExpiryDate (String expiryDate) {
        this.expiryDate = expiryDate;
    }

    public String getUserAuthenticationTime () {
        return userAuthenticationTime;
    }

    public void setUserAuthenticationTime (
            String userAuthenticationTime) {
        this.userAuthenticationTime = userAuthenticationTime;
    }

    public Set<String> getScopes () {
        return scopes;
    }

    public void setScopes (Set<String> scopes) {
        this.scopes = scopes;
    }

}

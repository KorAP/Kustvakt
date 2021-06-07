package de.ids_mannheim.korap.oauth2.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import de.ids_mannheim.korap.oauth2.constant.OAuth2ClientType;

/** Lists authorized OAuth2 clients of a user
 * 
 * @author margaretha
 *
 */
public class OAuth2UserClientDto {
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
    
    public String getClientName () {
        return clientName;
    }
    public void setClientName (String clientName) {
        this.clientName = clientName;
    }
    public String getClientId () {
        return clientId;
    }
    public void setClientId (String clientId) {
        this.clientId = clientId;
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
    public OAuth2ClientType getClientType () {
        return clientType;
    }
    public void setClientType (OAuth2ClientType clientType) {
        this.clientType = clientType;
    }
}

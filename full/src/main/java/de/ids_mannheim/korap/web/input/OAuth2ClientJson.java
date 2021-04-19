package de.ids_mannheim.korap.web.input;

import com.fasterxml.jackson.annotation.JsonProperty;

import de.ids_mannheim.korap.oauth2.constant.OAuth2ClientType;

/**
 * Defines attributes to register an OAuth2 client. Application name,
 * client type and description are required attributes. 
 * 
 * To accommodate desktop applications such as R, url and redirectURI
 * are not compulsory.
 * 
 * @author margaretha
 *
 */
public class OAuth2ClientJson {

    // required
    private String name;
    private OAuth2ClientType type;
    private String description;
    
    // optional
    private String url;
    // redirect URI determines where the OAuth 2.0 service will return
    // the user to after they have authorized a client.
    @JsonProperty("redirect_uri")
    private String redirectURI;
    

    public String getName () {
        return name;
    }

    public void setName (String name) {
        this.name = name;
    }

    public OAuth2ClientType getType () {
        return type;
    }

    public void setType (OAuth2ClientType type) {
        this.type = type;
    }

    public String getUrl () {
        return url;
    }

    public void setUrl (String url) {
        this.url = url;
    }

    public String getRedirectURI () {
        return redirectURI;
    }

    public void setRedirectURI (String redirectURI) {
        this.redirectURI = redirectURI;
    }

    public String getDescription () {
        return description;
    }

    public void setDescription (String description) {
        this.description = description;
    }
}

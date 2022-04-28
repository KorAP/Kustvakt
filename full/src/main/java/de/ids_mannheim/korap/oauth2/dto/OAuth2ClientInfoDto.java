package de.ids_mannheim.korap.oauth2.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.oauth2.constant.OAuth2ClientType;
import de.ids_mannheim.korap.oauth2.entity.OAuth2Client;
import de.ids_mannheim.korap.utils.JsonUtils;

/** Describes information about an OAuth2 client.
 * 
 * @author margaretha
 *
 */
@JsonInclude(Include.NON_EMPTY)
public class OAuth2ClientInfoDto {

    private String id;
    private String name;
    private String description;
    @JsonProperty("is_super")
    private String isSuper;
    private String url;
    private String redirect_uri;
    @JsonProperty("registered_by")
    private String registeredBy;
    @JsonProperty("registration_date")
    private String registrationDate;
    @JsonProperty("refresh_token_expiry")
    private int refreshTokenExpiry; // in seconds
    private OAuth2ClientType type;
    
    @JsonProperty("permitted")
    private boolean isPermitted;
    private JsonNode source;

    public OAuth2ClientInfoDto (OAuth2Client client) throws KustvaktException {
        this.id = client.getId();
        this.name = client.getName();
        this.description = client.getDescription();
        this.setType(client.getType());
        this.url = client.getUrl();
        this.registeredBy = client.getRegisteredBy();
        this.redirect_uri = client.getRedirectURI();
        this.registrationDate = client.getRegistrationDate().toString();
        this.isPermitted = client.isPermitted();
        String source = client.getSource();
        if (source != null) {
            this.source = JsonUtils.readTree(source);
        }
        if (client.isSuper()) {
            this.isSuper = "true";
        }
        this.refreshTokenExpiry = client.getRefreshTokenExpiry();
    }

    public String getId () {
        return id;
    }

    public void setId (String id) {
        this.id = id;
    }

    public String getName () {
        return name;
    }

    public void setName (String name) {
        this.name = name;
    }

    public String getDescription () {
        return description;
    }

    public void setDescription (String description) {
        this.description = description;
    }

    public String getIsSuper () {
        return isSuper;
    }

    public void setIsSuper (String isSuper) {
        this.isSuper = isSuper;
    }

    public String getUrl () {
        return url;
    }

    public void setUrl (String url) {
        this.url = url;
    }

    public String getRegisteredBy () {
        return registeredBy;
    }

    public void setRegisteredBy (String registeredBy) {
        this.registeredBy = registeredBy;
    }

    public OAuth2ClientType getType () {
        return type;
    }

    public void setType (OAuth2ClientType type) {
        this.type = type;
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
    
    public JsonNode getSource () {
        return source;
    }
    public void setSource (JsonNode source) {
        this.source = source;
    }
    
    public boolean isPermitted () {
        return isPermitted;
    }
    public void setPermitted (boolean isPermitted) {
        this.isPermitted = isPermitted;
    }
    
    public int getRefreshTokenExpiry () {
        return refreshTokenExpiry;
    }
    public void setRefreshTokenExpiry (int refreshTokenExpiry) {
        this.refreshTokenExpiry = refreshTokenExpiry;
    }

}

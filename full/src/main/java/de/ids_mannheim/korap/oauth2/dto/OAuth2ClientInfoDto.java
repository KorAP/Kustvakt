package de.ids_mannheim.korap.oauth2.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import de.ids_mannheim.korap.oauth2.constant.OAuth2ClientType;
import de.ids_mannheim.korap.oauth2.entity.OAuth2Client;

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
    private String isSuper;
    private String redirectURI;
    private String registeredBy;
    private OAuth2ClientType type;

    public OAuth2ClientInfoDto (OAuth2Client client) {
        this.id = client.getId();
        this.name = client.getName();
        this.description = client.getDescription();
        this.setType(client.getType());
        this.redirectURI = client.getRedirectURI();
        this.registeredBy = client.getRegisteredBy();

        if (client.isSuper()) {
            this.isSuper = "true";
        }
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

    public String getRedirectURI () {
        return redirectURI;
    }

    public void setRedirectURI (String redirectURI) {
        this.redirectURI = redirectURI;
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

}

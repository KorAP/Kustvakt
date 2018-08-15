package de.ids_mannheim.korap.oauth2.entity;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import de.ids_mannheim.korap.oauth2.constant.OAuth2ClientType;

/**
 * @author margaretha
 *
 */
@Entity
@Table(name = "oauth2_client")
public class OAuth2Client {

    @Id
    private String id;
    private String name;
    // Secret hashcode is stored instead of plain secret
    private String secret;
    @Enumerated(EnumType.STRING)
    private OAuth2ClientType type;
    @Column(name = "super")
    private boolean isSuper;
    @Column(name = "redirect_uri")
    private String redirectURI;
    @Column(name = "registered_by")
    private String registeredBy;
    private String description;

    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "url_id")
    private OAuth2ClientUrl clientUrl;

    @Override
    public String toString () {
        return "id=" + id + ", name=" + name + ", secret=" + secret + ", type="
                + type + ", isSuper=" + isSuper() + ", redirectURI="
                + redirectURI + ", registeredBy=" + registeredBy
                + ", description=" + description;
    }

    public boolean isSuper () {
        return isSuper;
    }

    public void setSuper (boolean isSuper) {
        this.isSuper = isSuper;
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

    public String getSecret () {
        return secret;
    }

    public void setSecret (String secret) {
        this.secret = secret;
    }

    public OAuth2ClientType getType () {
        return type;
    }

    public void setType (OAuth2ClientType type) {
        this.type = type;
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

    public String getDescription () {
        return description;
    }

    public void setDescription (String description) {
        this.description = description;
    }

    public OAuth2ClientUrl getClientUrl () {
        return clientUrl;
    }

    public void setClientUrl (OAuth2ClientUrl clientUrl) {
        this.clientUrl = clientUrl;
    }
}

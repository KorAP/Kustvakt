package de.ids_mannheim.korap.oauth2.entity;

import java.time.ZonedDateTime;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import de.ids_mannheim.korap.entity.InstalledPlugin;
import de.ids_mannheim.korap.oauth2.constant.OAuth2ClientType;

/** Describe oauth2_client database table mapping.
 * 
 * @author margaretha
 *
 */
@Entity
@Table(name = "oauth2_client")
public class OAuth2Client implements Comparable<OAuth2Client>{

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
    @Column(name = "registered_by", updatable = false)
    private String registeredBy;
    @Column(name = "registration_date", updatable = false)
    private ZonedDateTime registrationDate;
    
    // How long a refresh token for this client should be valid
    // in seconds. Maximum 31536000 seconds equivalent to 1 year     
    @Column(name = "refresh_token_expiry")
    private int refreshTokenExpiry;
    
    private String description;
    private String url;

    private String source;
    @Column(name = "is_permitted")
    private boolean isPermitted;
    
    @OneToMany(fetch = FetchType.LAZY, mappedBy = "client")
    private List<RefreshToken> refreshTokens;
    
    @OneToMany(fetch = FetchType.LAZY, mappedBy = "client")
    private List<AccessToken> accessTokens;
    
    @OneToMany(fetch = FetchType.LAZY, mappedBy = "client")
    private List<InstalledPlugin> installedPlugins;
    
    @Override
    public String toString () {
        return "id=" + id + ", name=" + name + ", secret=" + secret + ", type="
                + type + ", isSuper=" + isSuper() + ", redirectURI="
                + redirectURI + ", registeredBy=" + registeredBy
                + ", description=" + description;
    }

    @Override
    public int compareTo (OAuth2Client o) {
        return this.getName().compareTo(o.getName());
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
    
    public ZonedDateTime getRegistrationDate () {
        return registrationDate;
    }
    public void setRegistrationDate (ZonedDateTime registrationDate) {
        this.registrationDate = registrationDate;
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

    public String getSource () {
        return source;
    }

    public void setSource (String source) {
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

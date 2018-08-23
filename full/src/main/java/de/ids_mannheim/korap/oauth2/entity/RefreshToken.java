package de.ids_mannheim.korap.oauth2.entity;

import java.time.ZonedDateTime;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

@Entity
@Table(name = "oauth2_refresh_token")
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    private String token;
    @Column(name = "created_date", updatable = false)
    private ZonedDateTime createdDate;
    @Column(name = "expiry_date", updatable = false)
    private ZonedDateTime expiryDate;
    @Column(name = "user_id")
    private String userId;
    @Column(name = "client_id")
    private String clientId;
    @Column(name = "user_auth_time", updatable = false)
    private ZonedDateTime userAuthenticationTime;
    @Column(name = "is_revoked")
    private boolean isRevoked;

    @OneToMany(fetch = FetchType.EAGER, mappedBy = "refreshToken")
    private Set<AccessToken> accessTokens;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "oauth2_refresh_token_scope",
            joinColumns = @JoinColumn(name = "token_id",
                    referencedColumnName = "id"),
            inverseJoinColumns = @JoinColumn(name = "scope_id",
                    referencedColumnName = "id"),
            uniqueConstraints = @UniqueConstraint(
                    columnNames = { "token_id", "scope_id" }))
    private Set<AccessScope> scopes;

    public String getToken () {
        return token;
    }

    public void setToken (String token) {
        this.token = token;
    }

    public ZonedDateTime getCreatedDate () {
        return createdDate;
    }

    public void setCreatedDate (ZonedDateTime createdDate) {
        this.createdDate = createdDate;
    }

    public ZonedDateTime getExpiryDate () {
        return expiryDate;
    }

    public void setExpiryDate (ZonedDateTime expiryDate) {
        this.expiryDate = expiryDate;
    }

    public String getUserId () {
        return userId;
    }

    public void setUserId (String userId) {
        this.userId = userId;
    }

    public String getClientId () {
        return clientId;
    }

    public void setClientId (String clientId) {
        this.clientId = clientId;
    }

    public ZonedDateTime getUserAuthenticationTime () {
        return userAuthenticationTime;
    }

    public void setUserAuthenticationTime (
            ZonedDateTime userAuthenticationTime) {
        this.userAuthenticationTime = userAuthenticationTime;
    }

    public boolean isRevoked () {
        return isRevoked;
    }

    public void setRevoked (boolean isRevoked) {
        this.isRevoked = isRevoked;
    }

    public Set<AccessToken> getAccessTokens () {
        return accessTokens;
    }

    public void setAccessTokens (Set<AccessToken> accessTokens) {
        this.accessTokens = accessTokens;
    }

    public Set<AccessScope> getScopes () {
        return scopes;
    }

    public void setScopes (Set<AccessScope> scopes) {
        this.scopes = scopes;
    }

}

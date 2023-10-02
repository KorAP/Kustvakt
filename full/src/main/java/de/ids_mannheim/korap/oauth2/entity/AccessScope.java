package de.ids_mannheim.korap.oauth2.entity;

import java.io.Serializable;
import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;

import de.ids_mannheim.korap.constant.OAuth2Scope;
import lombok.Getter;
import lombok.Setter;

/**
 * Defines the oauth2_access_scope database table mapping and access
 * scope relations to Authorization, AccessToken and RefreshToken.
 * 
 * @author margaretha
 *
 */
@Getter
@Setter
@Entity
@Table(name = "oauth2_access_scope")
public class AccessScope implements Serializable {

    private static final long serialVersionUID = -7356877266702636705L;

    @Id
    @Enumerated(EnumType.STRING)
    private OAuth2Scope id;

    public AccessScope () {}

    public AccessScope (OAuth2Scope scope) {
        this.id = scope;
    }

    @ManyToMany(mappedBy = "scopes", fetch = FetchType.LAZY)
    private List<Authorization> authorizations;

    @ManyToMany(mappedBy = "scopes", fetch = FetchType.LAZY)
    private List<AccessToken> accessTokens;

    @ManyToMany(mappedBy = "scopes", fetch = FetchType.LAZY)
    private List<RefreshToken> refreshTokens;

    @Override
    public String toString () {
        return id.toString();
    }

    @Override
    public boolean equals (Object obj) {
        AccessScope scope = (AccessScope) obj;
        if (scope.getId().equals(this.id)) {
            return true;
        }

        return false;
    }
    
@Override
    public int hashCode () {
        return this.getId().hashCode();
    }
}

package de.ids_mannheim.korap.oauth2.entity;

import java.io.Serializable;
import java.util.List;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.Table;

import de.ids_mannheim.korap.oauth2.constant.OAuth2Scope;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "oauth2_access_scope")
public class AccessScope implements Serializable{

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
}

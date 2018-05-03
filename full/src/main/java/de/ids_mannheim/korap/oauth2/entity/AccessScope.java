package de.ids_mannheim.korap.oauth2.entity;

import java.util.List;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.Table;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "oauth2_access_scope")
public class AccessScope {

    @Id
    private String id;

    public AccessScope () {}

    public AccessScope (String scope) {
        this.id = scope;
    }

    @ManyToMany(mappedBy = "scopes", fetch = FetchType.LAZY)
    private List<Authorization> authorizations;
    
    @ManyToMany(mappedBy = "scopes", fetch = FetchType.LAZY)
    private List<AccessToken> accessTokens;

    @Override
    public String toString () {
        return id;
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

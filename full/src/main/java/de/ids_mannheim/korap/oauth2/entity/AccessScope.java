package de.ids_mannheim.korap.oauth2.entity;

import java.util.List;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
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
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    private String name;

    @ManyToMany(mappedBy = "scopes", fetch = FetchType.LAZY)
    private List<Authorization> authorizationCodes;

    @Override
    public String toString () {
        return "id: " + id + ", name: " + name;
    }

    @Override
    public boolean equals (Object obj) {
        String scope = (String) obj;
        if (scope.equals(this.name)) {
            return true;
        }

        return false;
    }
}

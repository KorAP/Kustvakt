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
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "oauth2_authorization")
public class Authorization {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    private String code;
    @Column(name = "client_id")
    private String clientId;
    @Column(name = "user_id")
    private String userId;
    @Column(name = "redirect_uri")
    private String redirectURI;
    @Column(name = "created_date", updatable=false)
    private ZonedDateTime createdDate;
    @Column(name = "is_revoked")
    private boolean isRevoked;
    @Column(name = "total_attempts")
    private int totalAttempts;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "oauth2_authorization_scope",
            joinColumns = @JoinColumn(name = "authorization_id",
                    referencedColumnName = "id"),
            inverseJoinColumns = @JoinColumn(name = "scope_id",
                    referencedColumnName = "id"),
            uniqueConstraints = @UniqueConstraint(
                    columnNames = { "authorization_id", "scope_id" }))
    private Set<AccessScope> scopes;
    
    @Override
    public String toString () {
        return "code: " + code + ", " + "clientId: " + clientId + ", "
                + "userId: " + userId + ", " + "createdDate: " + createdDate
                + ", " + "isRevoked: " + isRevoked + ", " + "scopes: " + scopes
                + ", " + "totalAttempts: " + totalAttempts;
    }
}

package de.ids_mannheim.korap.oauth2.entity;

import java.time.ZonedDateTime;
import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import lombok.Getter;
import lombok.Setter;

/**
 * Describes oauth2_authorization database table mapping and
 * authorization relations to AccessScope.
 * 
 * @author margaretha
 *
 */
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
    @Column(name = "created_date", updatable = false)
    private ZonedDateTime createdDate;
    @Column(name = "expiry_date")
    private ZonedDateTime expiryDate;
    @Column(name = "is_revoked")
    private boolean isRevoked;
    @Column(name = "total_attempts")
    private int totalAttempts;
    @Column(name = "user_auth_time", updatable = false)
    private ZonedDateTime userAuthenticationTime;
    @Column(updatable = false)
    private String nonce;

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

package de.ids_mannheim.korap.oauth2.entity;

import java.io.Serializable;
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
@Table(name = "oauth2_access_token")
public class AccessToken implements Serializable{

    private static final long serialVersionUID = 8452701765986475302L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    private String token;
    @Column(name = "created_date")
    private ZonedDateTime createdDate;
    @Column(name = "user_id")
    private String userId;
    @Column(name = "client_id")
    private String clientId;
    @Column(name = "is_revoked")
    private boolean isRevoked;
    @Column(name = "user_auth_time", updatable = false)
    private ZonedDateTime userAuthenticationTime;
    
//    @OneToOne(fetch=FetchType.LAZY, cascade=CascadeType.REMOVE)
//    @JoinColumn(name="authorization_id")
//    private Authorization authorization;
    
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "oauth2_access_token_scope",
            joinColumns = @JoinColumn(name = "token_id",
                    referencedColumnName = "id"),
            inverseJoinColumns = @JoinColumn(name = "scope_id",
                    referencedColumnName = "id"),
            uniqueConstraints = @UniqueConstraint(
                    columnNames = { "token_id", "scope_id" }))
    private Set<AccessScope> scopes;
    
}

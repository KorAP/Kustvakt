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
import lombok.Getter;
import lombok.Setter;

/**
 * @author margaretha
 *
 */
@Getter
@Setter
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
    @Column(name = "native")
    private boolean isNative;
    @Column(name = "redirect_uri")
    private String redirectURI;
    @Column(name = "registered_by")
    private String registeredBy;
    private String description;

    @OneToOne(fetch = FetchType.LAZY, cascade=CascadeType.ALL)
    @JoinColumn(name = "url_id")
    private OAuth2ClientUrl clientUrl;

    @Override
    public String toString () {
        return "id=" + id + ", name=" + name + ", secret=" + secret + ", type="
                + type + ", isNative=" + isNative + ", redirectURI="
                + redirectURI + ", registeredBy=" + registeredBy
                + ", description=" + description;
    }
}

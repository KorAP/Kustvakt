package de.ids_mannheim.korap.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Table;

import de.ids_mannheim.korap.constant.OAuth2ClientType;
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
    private String url;
    @Column(name = "url_hashcode")
    private int urlHashCode;
    @Column(name = "redirect_uri")
    private String redirectURI;
    @Column(name = "registered_by")
    private String registeredBy;


    @Override
    public String toString () {
        return "id=" + id + ", secret=" + secret + ", type=" + type + ", name="
                + name + ", url=" + url + ", redirectURI=" + redirectURI
                + ", registeredBy=" + registeredBy;
    }
}

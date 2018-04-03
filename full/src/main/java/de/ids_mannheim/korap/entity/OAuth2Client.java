package de.ids_mannheim.korap.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Table;

import de.ids_mannheim.korap.constant.ClientType;
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
    private String secret;
    @Enumerated(EnumType.STRING)
    private ClientType type;
    @Column(name = "redirect_uri")
    private String redirectURI;
    private String url;
    private String name;

    @Override
    public String toString () {
        return "id=" + id + ", secret=" + secret + ", type=" + type + ", name="
                + name + ", url=" + url + ", redirectURI=" + redirectURI;
    }
}

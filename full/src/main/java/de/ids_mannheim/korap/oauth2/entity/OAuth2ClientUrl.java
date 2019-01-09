package de.ids_mannheim.korap.oauth2.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import lombok.Getter;
import lombok.Setter;

/** Describes oauth2_client_url database table mapping
 * 
 * @author margaretha
 *
 */
@Getter
@Setter
@Entity
@Table(name = "oauth2_client_url")
public class OAuth2ClientUrl {

    @Id
    @Column(name = "url_hashcode")
    private int urlHashCode;
    private String url;

    @Override
    public String toString () {
        return "url_hashcode="+urlHashCode+", url=" + url;
    }
}

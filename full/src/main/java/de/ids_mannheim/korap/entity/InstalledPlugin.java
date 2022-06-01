package de.ids_mannheim.korap.entity;

import java.time.ZonedDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import de.ids_mannheim.korap.oauth2.entity.OAuth2Client;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Entity
@Table(name = "installed_plugin")
public class InstalledPlugin implements Comparable<InstalledPlugin>{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    @Column(name = "installed_by")
    private String installedBy;
    @Column(name = "installed_date")
    private ZonedDateTime installedDate;
    
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "client_id")
    private OAuth2Client client;
    
    // where a plugin is installed
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "super_client_id")
    private OAuth2Client superClient;

    @Override
    public int compareTo (InstalledPlugin o) {
        return this.client.compareTo(o.client);
    }
}

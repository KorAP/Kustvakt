package de.ids_mannheim.korap.entity;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * Describes default_setting table. Each user may define one set of
 * default settings. The elements of default settings are
 * not strictly defined and thus are described as JSON strings.
 * 
 * Some elements that are often used may be adopted as columns.
 * 
 * Examples of the default settings' elements are foundry, layer and
 * number of results per page.
 * 
 * @author margaretha
 *
 */
@Entity
@Table(name = "default_setting")
public class DefaultSetting {

    @Id
    private String username;
    private String settings; // json string

    public DefaultSetting () {}

    public DefaultSetting (String username, String settings) {
        this.username = username;
        this.settings = settings;
    }

    @Override
    public String toString () {
        return "name= " + getUsername() + ", settings= " + getSettings();
    }

    public String getUsername () {
        return username;
    }

    public void setUsername (String username) {
        this.username = username;
    }

    public String getSettings () {
        return settings;
    }

    public void setSettings (String settings) {
        this.settings = settings;
    }

}

package de.ids_mannheim.korap.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import de.ids_mannheim.korap.entity.InstalledPlugin;
import de.ids_mannheim.korap.oauth2.entity.OAuth2Client;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@JsonInclude(Include.NON_EMPTY)
public class InstalledPluginDto {
    private String client_id; // oauth2 client id
    private String name;
    private String description;
    private String url;
    @JsonProperty("installed_date")
    private String installedDate;
    
    public InstalledPluginDto (InstalledPlugin plugin) {
        OAuth2Client client = plugin.getClient();
        setClient_id(client.getId());
        setInstalledDate(plugin.getInstalledDate().toString());
        setName(client.getName());
        setDescription(client.getDescription());
        setUrl(client.getUrl());
    }
}

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
    @JsonProperty("client_id")
    private String clientId; // oauth2 client id
    @JsonProperty("super_client_id")
    private String superClientId;
    private String name;
    private String description;
    private String url;
    @JsonProperty("redirect_uri")
    private String redirectUri;
    @JsonProperty("installed_date")
    private String installedDate;
    
    public InstalledPluginDto (InstalledPlugin plugin) {
        OAuth2Client client = plugin.getClient();
        setClientId(client.getId());
        setSuperClientId(plugin.getSuperClient().getId());
        setInstalledDate(plugin.getInstalledDate().toString());
        setName(client.getName());
        setDescription(client.getDescription());
        setUrl(client.getUrl());
        setRedirectUri(client.getRedirectURI());
    }
}

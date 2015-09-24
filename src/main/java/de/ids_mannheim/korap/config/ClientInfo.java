package de.ids_mannheim.korap.config;

import lombok.Data;

/**
 * @author hanl
 * @date 22/01/2014
 */
@Data
public class ClientInfo {

    private Integer id;
    private String client_id;
    private String application_name;
    private boolean confidential;
    private String client_secret;
    // fixme: keep type?
    private String client_type;
    private String url;
    private String redirect_uri;

    public ClientInfo(String client_id, String client_secret) {
        this.client_id = client_id;
        this.client_secret = client_secret;
    }

    public String toJSON() {
        return "client_id: " + client_id + "\n" +
                "application_name: " + application_name + "\n" +
                "url: " + url + "\n" +
                "redirect_uri: " + redirect_uri + "\n";
    }

    //todo:
    public static ClientInfo fromJSON(String json) {
        return null;
    }
}

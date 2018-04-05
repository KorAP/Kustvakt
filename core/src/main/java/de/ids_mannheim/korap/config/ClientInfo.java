package de.ids_mannheim.korap.config;

import com.fasterxml.jackson.databind.node.ObjectNode;

import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.utils.JsonUtils;
import lombok.Data;

/**
 * @author hanl
 * @date 22/01/2014
 */
@Deprecated
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


    public ClientInfo (String client_id, String client_secret) {
        this.client_id = client_id;
        this.client_secret = client_secret;
    }


    public String toJSON () throws KustvaktException {
        ObjectNode node = JsonUtils.createObjectNode();
        node.put("client_id", client_id);
        node.put("client_secret", client_secret);
        node.put("application_name", application_name);
        node.put("url", url);
        node.put("redirect_uri", redirect_uri);
        return JsonUtils.toJSON(node);
    }


    public String toString () {
        return "client_id: " + client_id + "\n" + "application_name: "
                + application_name + "\n" + "url: " + url + "\n"
                + "redirect_uri: " + redirect_uri + "\n";
    }


    //todo:
    public static ClientInfo fromJSON (String json) {
        return null;
    }
}

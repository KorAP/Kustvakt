package de.ids_mannheim.korap.web;

import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.exceptions.StatusCodes;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MultivaluedMap;
import java.net.URI;
import java.util.List;
import java.util.Map;

/**
 * @author hanl
 * @date 10/12/2013
 */
// use for Piotr Ps. rest api connection
public class ClientsHandler {

    private WebTarget service;


    public ClientsHandler (URI address) {
        Client client = ClientBuilder.newClient();
        this.service = client.target(address);
    }


    public String getResponse (String path, String key, Object value)
            throws KustvaktException {
        try {
            return service.path(path).queryParam(key, value).request().get(String.class);
        }
        catch (WebApplicationException e) {
            throw new KustvaktException(StatusCodes.REQUEST_INVALID);
        }
    }


    public String getResponse (MultivaluedMap<String, String> map, String ... paths)
            throws KustvaktException {
        try {
            WebTarget resource = service;
            for (String p : paths)
                resource = resource.path(p);
            for (Map.Entry<String, List<String>> e : map.entrySet()) {
                for (String value : e.getValue())
                    resource = resource.queryParam(e.getKey(), value);
            }
            return resource.request().get(String.class);
        }
        catch (WebApplicationException e) {
            throw new KustvaktException(StatusCodes.REQUEST_INVALID);
        }
    }

}

package de.ids_mannheim.korap.web;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.core.util.MultivaluedMapImpl;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.exceptions.StatusCodes;

import javax.ws.rs.core.MultivaluedMap;
import java.net.URI;

/**
 * @author hanl
 * @date 10/12/2013
 */
// use for Piotr Ps. rest api connection
public class ClientsHandler {

    private WebResource service;


    public ClientsHandler (URI address) {
        ClientConfig config = new DefaultClientConfig();
        Client client = Client.create(config);
        this.service = client.resource(address);
    }


    public String getResponse (String path, String key, Object value)
            throws KustvaktException {
        MultivaluedMap map = new MultivaluedMapImpl();
        map.add(key, value);
        try {
            return service.path(path).queryParams(map).get(String.class);
        }
        catch (UniformInterfaceException e) {
            throw new KustvaktException(StatusCodes.REQUEST_INVALID);
        }
    }


    public String getResponse (MultivaluedMap map, String ... paths)
            throws KustvaktException {
        try {
            WebResource resource = service;
            for (String p : paths)
                resource = resource.path(p);
            resource = resource.queryParams(map);
            return resource.get(String.class);
        }
        catch (UniformInterfaceException e) {
            throw new KustvaktException(StatusCodes.REQUEST_INVALID);
        }
    }

}

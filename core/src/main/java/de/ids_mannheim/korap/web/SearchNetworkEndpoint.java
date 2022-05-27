package de.ids_mannheim.korap.web;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import org.apache.http.HttpStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import de.ids_mannheim.korap.config.KustvaktConfiguration;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.exceptions.StatusCodes;

@Service
public class SearchNetworkEndpoint {

    private final static Logger jlog = LogManager
            .getLogger(SearchNetworkEndpoint.class);

    @Autowired
    private KustvaktConfiguration config;

    public String search (String query) throws KustvaktException {
        String networkEndpointURL = config.getNetworkEndpointURL();
        if (networkEndpointURL == null || networkEndpointURL.isEmpty()) {
            throw new KustvaktException(
                    StatusCodes.NETWORK_ENDPOINT_NOT_AVAILABLE,
                    "Network endpoint is not available");
        }
        else {
            try {
                URL url = new URL(networkEndpointURL);
                HttpURLConnection connection = (HttpURLConnection) url
                        .openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type",
                        "application/json; charset=UTF-8");
                connection.setRequestProperty("Accept", "application/json");
                connection.setDoOutput(true);
                OutputStream os = connection.getOutputStream();
                byte[] input = query.getBytes("utf-8");
                os.write(input, 0, input.length);

                String entity = null;
                if (connection.getResponseCode() == HttpStatus.SC_OK) {
                    BufferedReader br = new BufferedReader(
                            new InputStreamReader(connection.getInputStream(),
                                    "utf-8"));
                    StringBuilder response = new StringBuilder();
                    String responseLine = null;
                    while ((responseLine = br.readLine()) != null) {
                        response.append(responseLine.trim());
                    }
                    entity = response.toString();
                }

                if (entity != null && !entity.isEmpty()) {
                    return entity;
                }
                else {
                    String message = connection.getResponseCode() + " "
                            + connection.getResponseMessage();
                    jlog.warn("Search on network endpoint failed "
                            + networkEndpointURL + ". Message: " + message);

                    throw new KustvaktException(
                            StatusCodes.SEARCH_NETWORK_ENDPOINT_FAILED,
                            "Failed searching at network endpoint: "
                                    + networkEndpointURL,
                            message);
                }
            }
            catch (Exception e) {
                throw new KustvaktException(
                        StatusCodes.SEARCH_NETWORK_ENDPOINT_FAILED,
                        "Failed searching at network endpoint: "
                                + networkEndpointURL,
                        e.getCause());
            }
        }
    }
}

package de.ids_mannheim.korap.oauth2.service;

import java.io.File;
import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.oauth2.dao.OAuth2ClientDao;
import de.ids_mannheim.korap.oauth2.dto.OAuth2ClientDto;
import de.ids_mannheim.korap.oauth2.entity.OAuth2Client;
import de.ids_mannheim.korap.utils.JsonUtils;
import de.ids_mannheim.korap.web.input.OAuth2ClientJson;

@Service
public class OAuth2InitClientService {

    private static Logger log =
            LogManager.getLogger(OAuth2InitClientService.class);
    public static String OAUTH2_CLIENT_JSON_INPUT_FILE =
            "initial_super_client.json";
    public static String OUTPUT_FOLDER = "client";
    public static String OUTPUT_FILENAME = "super_client_info";
    public static String TEST_OUTPUT_FILENAME = "test_super_client_info";

    @Autowired
    private OAuth2ClientService clientService;
    @Autowired
    private OAuth2ClientDao clientDao;

    public void createInitialSuperClient (String outputFilename)
            throws IOException, KustvaktException {

        File dir = new File(OUTPUT_FOLDER);
        if (!dir.exists()) {
            dir.mkdir();
        }

        String path = OUTPUT_FOLDER + "/" + outputFilename;
        File f = new File(path);

        if (!f.exists()) {
            OAuth2ClientJson json = JsonUtils.readFile(
                    OAUTH2_CLIENT_JSON_INPUT_FILE, OAuth2ClientJson.class);
            OAuth2ClientDto clientDto =
                    clientService.registerClient(json, "system");
            String clientId = clientDto.getClient_id();
            OAuth2Client client = clientService.retrieveClient(clientId);
            client.setSuper(true);
            clientDao.updateClient(client);
            JsonUtils.writeFile(path, clientDto);
        }
        else {
            log.warn(
                    "Super client info file exists. Initial super client registration is cancelled.");
        }

    }

    public void createInitialTestSuperClient () throws IOException, KustvaktException {
        createInitialSuperClient(TEST_OUTPUT_FILENAME);
    }
}

package de.ids_mannheim.korap.service;

import org.apache.commons.validator.routines.UrlValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import de.ids_mannheim.korap.constant.ClientType;
import de.ids_mannheim.korap.dao.OAuth2ClientDao;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.exceptions.StatusCodes;
import de.ids_mannheim.korap.interfaces.EncryptionIface;
import de.ids_mannheim.korap.web.input.OAuth2ClientJson;

@Service
public class OAuth2ClientService {

    @Autowired
    private OAuth2ClientDao clientDao;
    @Autowired
    private UrlValidator urlValidator;
    @Autowired
    private EncryptionIface encryption;


    public void registerClient (OAuth2ClientJson clientJson)
            throws KustvaktException {
        if (!urlValidator.isValid(clientJson.getUrl())) {
            throw new KustvaktException(StatusCodes.INVALID_ARGUMENT,
                    clientJson.getUrl() + " is invalid.", clientJson.getUrl());
        }
        if (!urlValidator.isValid(clientJson.getRedirectURI())) {
            throw new KustvaktException(StatusCodes.INVALID_ARGUMENT,
                    clientJson.getRedirectURI() + " is invalid.",
                    clientJson.getRedirectURI());
        }

        String secret = null;
        if (clientJson.getType().equals(ClientType.CONFIDENTIAL)) {
            secret = encryption.createToken();
        }

        String id = encryption.createRandomNumber();
        
        clientDao.registerClient(id, secret, clientJson.getName(),
                clientJson.getType(), clientJson.getUrl(),
                clientJson.getRedirectURI());
    }
}

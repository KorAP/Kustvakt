package de.ids_mannheim.korap.oauth2.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.exceptions.StatusCodes;
import de.ids_mannheim.korap.oauth2.constant.OAuth2ClientType;
import de.ids_mannheim.korap.oauth2.dao.AccessTokenDao;
import de.ids_mannheim.korap.oauth2.dao.OAuth2ClientDao;
import de.ids_mannheim.korap.oauth2.dao.RefreshTokenDao;
import de.ids_mannheim.korap.oauth2.entity.OAuth2Client;

@Service
public class OAuth2AdminService {

    @Autowired
    private OAuth2ClientService clientService;

    @Autowired
    private AccessTokenDao tokenDao;
    @Autowired
    private RefreshTokenDao refreshDao;
    @Autowired
    private OAuth2ClientDao clientDao;

    public void cleanTokens () {
        tokenDao.deleteInvalidAccessTokens();
        refreshDao.deleteInvalidRefreshTokens();
        tokenDao.clearCache();
    }

    public void updatePrivilege (String clientId, boolean isSuper)
            throws KustvaktException {

        OAuth2Client client = clientDao.retrieveClientById(clientId);
        if (isSuper) {
            if (!client.getType().equals(OAuth2ClientType.CONFIDENTIAL)) {
                throw new KustvaktException(StatusCodes.NOT_ALLOWED,
                        "Only confidential clients are allowed to be super clients.");
            }
        }
        else {
            clientService.revokeAllAuthorizationsByClientId(clientId);
        }

        client.setSuper(isSuper);
        clientDao.updateClient(client);
    }
}

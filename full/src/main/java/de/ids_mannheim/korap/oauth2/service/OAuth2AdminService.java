package de.ids_mannheim.korap.oauth2.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import de.ids_mannheim.korap.oauth2.dao.AccessTokenDao;
import de.ids_mannheim.korap.oauth2.dao.RefreshTokenDao;

@Service
public class OAuth2AdminService {

    @Autowired
    private AccessTokenDao tokenDao;
    @Autowired
    private RefreshTokenDao refreshDao;
 
    
    public void cleanTokens () {
        tokenDao.deleteInvalidAccessTokens();
        refreshDao.deleteInvalidRefreshTokens();
    }

    
}

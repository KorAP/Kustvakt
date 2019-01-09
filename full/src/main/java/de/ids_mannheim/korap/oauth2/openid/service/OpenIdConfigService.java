package de.ids_mannheim.korap.oauth2.openid.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import de.ids_mannheim.korap.config.FullConfiguration;
import de.ids_mannheim.korap.oauth2.openid.OpenIdConfiguration;

/**
 * @author margaretha
 *
 */
@Service
public class OpenIdConfigService {

    @Autowired
    private FullConfiguration config;

    public OpenIdConfiguration retrieveOpenIdConfigInfo () {
        return config.getOpenidConfig();
    }
}

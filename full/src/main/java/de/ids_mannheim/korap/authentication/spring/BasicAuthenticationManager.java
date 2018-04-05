package de.ids_mannheim.korap.authentication.spring;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;

import de.ids_mannheim.korap.config.Attributes;
import de.ids_mannheim.korap.config.KustvaktConfiguration;
import de.ids_mannheim.korap.config.Scopes;
import de.ids_mannheim.korap.constant.TokenType;
import de.ids_mannheim.korap.dao.UserDao;
import de.ids_mannheim.korap.interfaces.EncryptionIface;
import de.ids_mannheim.korap.security.context.TokenContext;
import de.ids_mannheim.korap.user.KorAPUser;
import de.ids_mannheim.korap.user.User;
import de.ids_mannheim.korap.utils.TimeUtils;

/** * Basic authentication manager is intended to be used with a database. 
 * It is currently only used for testing using a dummy DAO (@see {@link UserDao}) 
 * without passwords.
 * 
 * @author margaretha
 *
 */
public class BasicAuthenticationManager implements AuthenticationManager{

    @Autowired
    private KustvaktConfiguration config;
    @Autowired
    private EncryptionIface crypto;
    @Autowired
    private UserDao dao;
    
    @Override
    public Authentication authenticate (Authentication authentication)
            throws AuthenticationException {

        String username = (String) authentication.getPrincipal();
        String password = (String) authentication.getCredentials();
        
        TokenContext c = new TokenContext();
        User user = dao.getAccount(username);
        if (user instanceof KorAPUser
                && ((KorAPUser) user).getPassword() != null) {
            boolean check = crypto.checkHash(password,
                    ((KorAPUser) user).getPassword());

            if (!check) return null;
        }
        
        c.setUsername(username);
        c.setExpirationTime(TimeUtils.plusSeconds(this.config.getTokenTTL())
                .getMillis());
        c.setTokenType(TokenType.BASIC);
        // todo: for production mode, set true
        c.setSecureRequired(false);
        // EM: is this secure?
        c.setToken(authentication.toString());
        c.addContextParameter(Attributes.SCOPES,
                Scopes.Scope.search.toString());
        
        return authentication;
    }

}

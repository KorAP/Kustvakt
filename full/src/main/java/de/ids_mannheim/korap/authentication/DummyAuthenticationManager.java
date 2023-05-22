package de.ids_mannheim.korap.authentication;

import java.util.Map;

import javax.ws.rs.core.HttpHeaders;

import org.springframework.beans.factory.annotation.Autowired;

import de.ids_mannheim.korap.config.KustvaktConfiguration;
import de.ids_mannheim.korap.constant.AuthenticationMethod;
import de.ids_mannheim.korap.constant.TokenType;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.security.context.TokenContext;
import de.ids_mannheim.korap.user.KorAPUser;
import de.ids_mannheim.korap.user.User;
import de.ids_mannheim.korap.utils.TimeUtils;

public class DummyAuthenticationManager extends AuthenticationManager {

    @Autowired
    private KustvaktConfiguration config;

    public DummyAuthenticationManager () {}
    
    @Override
    public TokenContext getTokenContext (TokenType type, String token,
            String host, String useragent) throws KustvaktException {
        TokenContext c = new TokenContext();
        c.setUsername("guest");
        c.setHostAddress(host);
        c.setUserAgent(useragent);
        c.setExpirationTime(
                TimeUtils.plusSeconds(config.getShortTokenTTL()).getMillis());
        c.setTokenType(TokenType.BASIC);
        c.setToken("dummyToken");
        return c;
    }

    @Override
    public User getUser (String username) throws KustvaktException {
        KorAPUser user = new KorAPUser();
        user.setUsername(username);
        return user;
    }

    @Override
    public User authenticate (AuthenticationMethod method, String username,
            String password, Map<String, Object> attributes)
            throws KustvaktException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public TokenContext createTokenContext (User user, Map<String, Object> attr,
            TokenType type) throws KustvaktException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void setAccessAndLocation (User user, HttpHeaders headers) {
        // TODO Auto-generated method stub

    }

    @Override
    public User getUser (String username, String method)
            throws KustvaktException {
        // TODO Auto-generated method stub
        return null;
    }

}

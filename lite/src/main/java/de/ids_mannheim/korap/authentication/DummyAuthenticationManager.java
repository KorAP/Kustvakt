package de.ids_mannheim.korap.authentication;

import java.util.Map;

import javax.ws.rs.core.HttpHeaders;

import org.springframework.beans.factory.annotation.Autowired;

import de.ids_mannheim.korap.config.KustvaktConfiguration;
import de.ids_mannheim.korap.constant.AuthenticationMethod;
import de.ids_mannheim.korap.constant.TokenType;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.security.context.TokenContext;
import de.ids_mannheim.korap.user.User;
import de.ids_mannheim.korap.user.Userdata;
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
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean isRegistered (String id) {
        // TODO Auto-generated method stub
        return false;
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
    public void logout (TokenContext context) throws KustvaktException {
        // TODO Auto-generated method stub

    }

    @Override
    public void lockAccount (User user) throws KustvaktException {
        // TODO Auto-generated method stub

    }

    @Override
    public User createUserAccount (Map<String, Object> attributes,
            boolean confirmation_required) throws KustvaktException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean deleteAccount (User user) throws KustvaktException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public <T extends Userdata> T getUserData (User user, Class<T> clazz)
            throws KustvaktException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void updateUserData (Userdata data) throws KustvaktException {
        // TODO Auto-generated method stub

    }

    @Override
    public Object[] validateResetPasswordRequest (String username, String email)
            throws KustvaktException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void resetPassword (String uriFragment, String username,
            String newPassphrase) throws KustvaktException {
        // TODO Auto-generated method stub

    }

    @Override
    public void confirmRegistration (String uriFragment, String username)
            throws KustvaktException {
        // TODO Auto-generated method stub

    }

    @Override
    public User getUser (String username, String method)
            throws KustvaktException {
        // TODO Auto-generated method stub
        return null;
    }

}

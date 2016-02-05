package de.ids_mannheim.korap.web.service;

import de.ids_mannheim.korap.config.AuthCodeInfo;
import de.ids_mannheim.korap.config.BeanConfiguration;
import de.ids_mannheim.korap.config.ClientInfo;
import de.ids_mannheim.korap.config.TestHelper;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.handlers.OAuth2Handler;
import de.ids_mannheim.korap.interfaces.EncryptionIface;
import de.ids_mannheim.korap.user.TokenContext;
import de.ids_mannheim.korap.user.User;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author hanl
 * @date 13/05/2015
 */

public class OAuth2HandlerTest {

    private static ClientInfo info;
    private static OAuth2Handler handler;
    private static EncryptionIface crypto;
    private static final String SCOPES = "search preferences queries account";
    private static User user;

    @BeforeClass
    public static void setup() throws KustvaktException {
        BeanConfiguration.loadClasspathContext("default-config.xml");
        handler = new OAuth2Handler(
                BeanConfiguration.getBeans().getPersistenceClient());
        crypto = BeanConfiguration.getBeans().getEncryption();
        info = new ClientInfo(crypto.createID(), crypto.createToken());
        info.setConfidential(true);
        //todo: support for subdomains?!
        info.setUrl("http://localhost:8080/api/v0.1");
        info.setRedirect_uri("testwebsite/login");

        TestHelper.setupAccount();
        user = TestHelper.getUser();
        handler.registerClient(info, user);
    }

    @AfterClass
    public static void drop() throws KustvaktException {
        assert handler != null;
        handler.removeClient(info, user);
        TestHelper.dropUser();
        BeanConfiguration.closeApplication();
    }

    @Test
    public void testStoreAuthorizationCodeThrowsNoException()
            throws KustvaktException {
        String auth_code = crypto.createToken();
        AuthCodeInfo codeInfo = new AuthCodeInfo(info.getClient_id(),
                auth_code);
        codeInfo.setScopes(SCOPES);

        handler.authorize(codeInfo, user);
        codeInfo = handler.getAuthorization(auth_code);
        Assert.assertNotNull("client is null!", codeInfo);
    }

    @Test
    public void testAuthorizationCodeRemoveThrowsNoException()
            throws KustvaktException {
        String auth_code = crypto.createToken();
        AuthCodeInfo codeInfo = new AuthCodeInfo(info.getClient_id(),
                auth_code);
        codeInfo.setScopes(SCOPES);

        handler.authorize(codeInfo, user);
        String t = crypto.createToken();
        String refresh = crypto.createToken();
        handler.addToken(codeInfo.getCode(), t, refresh, 7200);

        TokenContext ctx = handler.getContext(t);
        Assert.assertNotNull("context is null", ctx);

        AuthCodeInfo c2 = handler.getAuthorization(codeInfo.getCode());
        Assert.assertNull("clearing authorization failed", c2);
    }

    @Test
    public void testTokenEndpointRedirect() {

    }

    @Test
    public void testStoreAccessCodeViaAuthCodeThrowsNoException() {
        String auth_code = crypto.createToken();
        AuthCodeInfo codeInfo = new AuthCodeInfo(info.getClient_id(),
                auth_code);
        codeInfo.setScopes(SCOPES);

    }

    @Test
    public void testDeleteAccessCodesByUserDeleteCascade() {

    }

    @Test
    public void testAccessTokenbyUserDeleteCascade() {

    }

    @Test
    public void testRefreshToken() {

    }

    // fixme: exception thrown?!
    @Test
    public void testAccessTokenExpired() {

    }
}

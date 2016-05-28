package de.ids_mannheim.korap.web.service;

import de.ids_mannheim.korap.config.AuthCodeInfo;
import de.ids_mannheim.korap.config.BeanConfigTest;
import de.ids_mannheim.korap.config.ClientInfo;
import de.ids_mannheim.korap.config.ContextHolder;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.handlers.OAuth2Handler;
import de.ids_mannheim.korap.interfaces.EncryptionIface;
import de.ids_mannheim.korap.interfaces.db.PersistenceClient;
import de.ids_mannheim.korap.user.TokenContext;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;

/**
 * @author hanl
 * @date 13/05/2015
 */

public class OAuth2HandlerTest extends BeanConfigTest {

    private static ClientInfo info;

    private static final String SCOPES = "search preferences queries account";


    @BeforeClass
    public static void setup () throws Exception {
        //        BeanConfiguration.loadClasspathContext("default-config.xml");
        //        handler = new OAuth2Handler(
        //                BeanConfiguration.getKustvaktContext().getPersistenceClient());
        //        crypto = BeanConfiguration.getKustvaktContext().getEncryption();
        //        info = new ClientInfo(crypto.createID(), crypto.createToken());
        //        info.setConfidential(true);
        //        //todo: support for subdomains?!
        //        info.setUrl("http://localhost:8080/api/v0.1");
        //        info.setRedirect_uri("testwebsite/login");
        //
        //        helper = TestHelper.newInstance();
        //        helper.setupAccount();
        //        PersistenceClient cl = helper.getBean(ContextHolder.KUSTVAKT_DB);
        //        handler = new OAuth2Handler(cl);
        //        handler.registerClient(info, helper.getUser());
        //        crypto = helper.getBean(ContextHolder.KUSTVAKT_ENCRYPTION);
    }


    //    @AfterClass
    //    public static void drop() throws KustvaktException {
    //        assert handler != null;
    //        handler.removeClient(info, helper().getUser());
    //        helper().dropUser();
    //        BeansFactory.closeApplication();
    //    }

    @Test
    public void testStoreAuthorizationCodeThrowsNoException ()
            throws KustvaktException {
        String auth_code = helper().getContext().getEncryption().createToken();
        AuthCodeInfo codeInfo = new AuthCodeInfo(info.getClient_id(), auth_code);
        codeInfo.setScopes(SCOPES);

        OAuth2Handler handler = new OAuth2Handler(helper().getContext()
                .getPersistenceClient());
        handler.authorize(codeInfo, helper().getUser());
        codeInfo = handler.getAuthorization(auth_code);
        assertNotNull("client is null!", codeInfo);
    }


    @Test
    public void testAuthorizationCodeRemoveThrowsNoException ()
            throws KustvaktException {
        String auth_code = helper().getContext().getEncryption().createToken();
        AuthCodeInfo codeInfo = new AuthCodeInfo(info.getClient_id(), auth_code);
        codeInfo.setScopes(SCOPES);

        OAuth2Handler handler = new OAuth2Handler(helper().getContext()
                .getPersistenceClient());
        handler.authorize(codeInfo, helper().getUser());
        String t = helper().getContext().getEncryption().createToken();
        String refresh = helper().getContext().getEncryption().createToken();
        handler.addToken(codeInfo.getCode(), t, refresh, 7200);

        TokenContext ctx = handler.getContext(t);
        assertNotNull("context is null", ctx);

        AuthCodeInfo c2 = handler.getAuthorization(codeInfo.getCode());
        Assert.assertNull("clearing authorization failed", c2);
    }


    @Test
    public void testTokenEndpointRedirect () {

    }


    @Test
    public void testStoreAccessCodeViaAuthCodeThrowsNoException () {
        String auth_code = helper().getContext().getEncryption().createToken();
        AuthCodeInfo codeInfo = new AuthCodeInfo(info.getClient_id(), auth_code);
        codeInfo.setScopes(SCOPES);

    }


    @Test
    public void testDeleteAccessCodesByUserDeleteCascade () {

    }


    @Test
    public void testAccessTokenbyUserDeleteCascade () {

    }


    @Test
    public void testRefreshToken () {

    }


    // fixme: exception thrown?!
    @Test
    public void testAccessTokenExpired () {

    }


    @Override
    public void initMethod () throws KustvaktException {
        helper().setupAccount();

        EncryptionIface crypto = helper().getContext().getEncryption();
        info = new ClientInfo(crypto.createID(), crypto.createToken());
        info.setConfidential(true);
        //todo: support for subdomains?!
        info.setUrl("http://localhost:8080/api/v0.1");
        info.setRedirect_uri("testwebsite/login");
        PersistenceClient cl = helper().getBean(ContextHolder.KUSTVAKT_DB);
        OAuth2Handler handler = new OAuth2Handler(cl);
        handler.registerClient(info, helper().getUser());
    }
}

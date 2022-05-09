package de.ids_mannheim.korap.authentication;

import com.unboundid.ldap.listener.InMemoryDirectoryServer;
import com.unboundid.ldap.listener.InMemoryDirectoryServerConfig;
import com.unboundid.ldap.listener.InMemoryListenerConfig;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.util.Base64;
import com.unboundid.util.StaticUtils;
import com.unboundid.util.ssl.KeyStoreKeyManager;
import com.unboundid.util.ssl.SSLUtil;
import com.unboundid.util.ssl.TrustAllTrustManager;
import com.unboundid.util.ssl.TrustStoreTrustManager;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.security.GeneralSecurityException;

import static de.ids_mannheim.korap.authentication.LdapAuth3.*;
import static org.junit.Assert.assertEquals;

public class LdapAuth3Test {
    public static final String TEST_LDAP_CONF = "src/test/resources/test-ldap.conf";
    public static final String TEST_LDAPS_CONF = "src/test/resources/test-ldaps.conf";
    public static final String TEST_LDAPS_TS_CONF = "src/test/resources/test-ldaps-with-truststore.conf";
    public static final String TEST_LDAP_USERS_LDIF = "src/test/resources/test-ldap-users.ldif";
    private static final String keyStorePath = "src/test/resources/keystore.p12";
    static InMemoryDirectoryServer server;

    @BeforeClass
    public static void startDirectoryServer() throws LDAPException, GeneralSecurityException {
        InMemoryDirectoryServerConfig config = new InMemoryDirectoryServerConfig("dc=example,dc=com");
        config.addAdditionalBindCredentials("cn=admin,dc=example,dc=com", "adminpassword");
        config.setSchema(null);

        final SSLUtil serverSSLUtil = new SSLUtil(new KeyStoreKeyManager(keyStorePath, "password".toCharArray(), "PKCS12", "server-cert"), new TrustStoreTrustManager(keyStorePath));

        final SSLUtil clientSslUtil = new SSLUtil(new TrustAllTrustManager());

        config.setListenerConfigs(InMemoryListenerConfig.createLDAPConfig("LDAP", // Listener name
                        null, // Listen address. (null = listen on all interfaces)
                        3268, // Listen port (0 = automatically choose an available port)
                        clientSslUtil.createSSLSocketFactory()), // StartTLS factory
                InMemoryListenerConfig.createLDAPSConfig("LDAPS", // Listener name
                        null, // Listen address. (null = listen on all interfaces)
                        3269, // Listen port (0 = automatically choose an available port)
                        serverSSLUtil.createSSLServerSocketFactory(), clientSslUtil.createSSLSocketFactory()));
        server = new InMemoryDirectoryServer(config);

        String configPath = TEST_LDAP_USERS_LDIF;
        server.importFromLDIF(true, configPath);
        server.startListening();
    }

    @AfterClass
    public static void ShutDownDirectoryServer() {
        server.shutDown(true);
    }

    @Test
    public void loginWithExtraProfileNameWorks() throws LDAPException {
        assertEquals(LDAP_AUTH_ROK, LdapAuth3.login("testuser123", "password", TEST_LDAP_CONF));
    }

    @Test
    public void loginWithUidWorks() throws LDAPException {
        assertEquals(LDAP_AUTH_ROK, LdapAuth3.login("testuser", "password", TEST_LDAP_CONF));
    }

    @Test
    public void loginWithUidAndBase64PasswordWorks() throws LDAPException {
        final byte[] passwordBytes = StaticUtils.getBytes("password");
        String pw = Base64.encode(passwordBytes);
        assertEquals(LDAP_AUTH_ROK, LdapAuth3.login("testuser", pw, TEST_LDAP_CONF));
    }

    @Test
    public void loginWithEmailWorks() throws LDAPException {
        final byte[] passwordBytes = StaticUtils.getBytes("password");
        String pw = Base64.encode(passwordBytes);
        assertEquals(LDAP_AUTH_ROK, LdapAuth3.login("testuser@example.com", pw, TEST_LDAP_CONF));
    }

    @Test
    public void allLoginPasswordCombinationsWork() throws LDAPException {
        assertEquals(LDAP_AUTH_ROK, LdapAuth3.login("uid", "userPassword", TEST_LDAP_CONF));
        assertEquals(LDAP_AUTH_ROK, LdapAuth3.login("uid", "extraPassword", TEST_LDAP_CONF));
        assertEquals(LDAP_AUTH_ROK, LdapAuth3.login("mail@example.org", "userPassword", TEST_LDAP_CONF));
        assertEquals(LDAP_AUTH_ROK, LdapAuth3.login("mail@example.org", "extraPassword", TEST_LDAP_CONF));
        assertEquals(LDAP_AUTH_ROK, LdapAuth3.login("extraProfile", "userPassword", TEST_LDAP_CONF));
        assertEquals(LDAP_AUTH_ROK, LdapAuth3.login("extraProfile", "extraPassword", TEST_LDAP_CONF));
    }

    @Test
    public void loginWithWrongEmailFails() throws LDAPException {
        assertEquals(LDAP_AUTH_RUNKNOWN, LdapAuth3.login("notestuser@example.com", "topsecret", TEST_LDAP_CONF));
    }

    @Test
    public void loginWithEmailAndWrongPasswordFails() throws LDAPException {
        assertEquals(LDAP_AUTH_RUNKNOWN, LdapAuth3.login("testuser@example.com", "wrongpw", TEST_LDAP_CONF));
    }

    @Test
    public void loginWithUsernameAndWrongPasswordFails() throws LDAPException {
        assertEquals(LDAP_AUTH_RUNKNOWN, LdapAuth3.login("testuser", "wrongpw", TEST_LDAP_CONF));
    }

    @Test
    public void loginOfNotRegisteredUserFails() throws LDAPException {
        assertEquals(LDAP_AUTH_RNOTREG, LdapAuth3.login("not_registered_user", "topsecret", TEST_LDAP_CONF));
    }

    @Test
    public void blockedUserIsRefused() throws LDAPException {
        assertEquals(LDAP_AUTH_RLOCKED, LdapAuth3.login("nameOfBlockedUser", "topsecret", TEST_LDAP_CONF));
    }

    @Test
    public void loginWithUsernameOverSSLWorks() throws LDAPException {
        assertEquals(LDAP_AUTH_ROK, LdapAuth3.login("testuser", "password", TEST_LDAPS_CONF));
    }

    @Test
    public void loginOnTrustedServerWorks() throws LDAPException {
        assertEquals(LDAP_AUTH_ROK, LdapAuth3.login("testuser", "password", TEST_LDAPS_TS_CONF));
    }

    @Test
    public void loginOnTrustedServerWithWrongPassswordFails() throws LDAPException {
        assertEquals(LDAP_AUTH_RUNKNOWN, LdapAuth3.login("testuser", "topsecrets", TEST_LDAPS_TS_CONF));
    }

    @Test
    public void passwordWithAsteriskWorks() throws LDAPException {
        assertEquals(LDAP_AUTH_ROK, LdapAuth3.login("test", "top*ecret", TEST_LDAPS_CONF));
    }

    @Test
    public void passwordWithGlobOperatorFails() throws LDAPException {
        assertEquals(LDAP_AUTH_RUNKNOWN, LdapAuth3.login("testuser", "passw*", TEST_LDAPS_TS_CONF));
        assertEquals(LDAP_AUTH_ROK, LdapAuth3.login("testuser", "password", TEST_LDAPS_TS_CONF));
    }

    @Test
    public void passwordWithExistenceOperatorFails() throws LDAPException {
        assertEquals(LDAP_AUTH_RUNKNOWN, LdapAuth3.login("testuser", "*", TEST_LDAPS_TS_CONF));
        assertEquals(LDAP_AUTH_ROK, LdapAuth3.login("testuser", "password", TEST_LDAPS_TS_CONF));
    }

    @Test
    public void gettingMailAttributeForUid() throws LDAPException {
        assertEquals("testuser@example.com", LdapAuth3.getEmail("testuser", TEST_LDAP_CONF));
        assertEquals("peter@example.org", LdapAuth3.getEmail("testuser2", TEST_LDAPS_CONF));
        assertEquals(null, LdapAuth3.getEmail("non-exsting", TEST_LDAPS_CONF));
    }

    @Test
    public void gettingMailAttributeForNotRegisteredUserWorks() throws LDAPException {
        assertEquals("not_registered_user@example.com", LdapAuth3.getEmail("not_registered_user", TEST_LDAP_CONF));
    }

    @Test
    public void gettingMailAttributeForBlockedUserWorks() throws LDAPException {
        assertEquals("nameOfBlockedUser@example.com", LdapAuth3.getEmail("nameOfBlockedUser", TEST_LDAP_CONF));
    }

    @Test
    public void canLoadLdapConfig() {
        LDAPConfig ldapConfig = new LDAPConfig(TEST_LDAPS_CONF);
        assertEquals(3269, ldapConfig.port);
        assertEquals("localhost", ldapConfig.host);
    }
}

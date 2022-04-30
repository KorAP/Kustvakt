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

import static de.ids_mannheim.korap.authentication.LdapAuth3.LDAP_AUTH_ROK;
import static de.ids_mannheim.korap.authentication.LdapAuth3.LDAP_AUTH_RUNKNOWN;
import static org.junit.Assert.assertEquals;

public class LdapAuth3Test {
    public static final String TEST_LDAP_PROPERTIES = "src/test/resources/test-ldap.properties";
    public static final String TEST_LDAPS_PROPERTIES = "src/test/resources/test-ldaps.properties";
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
                        serverSSLUtil.createSSLServerSocketFactory(),
                        clientSslUtil.createSSLSocketFactory()));
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
    public void testLoginWithUsername() throws LDAPException {
        assertEquals(LDAP_AUTH_ROK, LdapAuth3.login("testuser", "topsecret", TEST_LDAP_PROPERTIES));
    }

    @Test
    public void testLoginWithUid() throws LDAPException {
        final byte[] passwordBytes = StaticUtils.getBytes("password");
        String pw = Base64.encode(passwordBytes);
        assertEquals(LDAP_AUTH_ROK, LdapAuth3.login("testuser", pw, TEST_LDAP_PROPERTIES));
    }

    @Test
    public void testLoginWithEmail() throws LDAPException {
        final byte[] passwordBytes = StaticUtils.getBytes("password");
        String pw = Base64.encode(passwordBytes);
        assertEquals(LDAP_AUTH_ROK, LdapAuth3.login("testuser@example.com", pw, TEST_LDAP_PROPERTIES));
    }

    @Test
    public void testFailingLoginWithWrongEmail() throws LDAPException {
        assertEquals(LDAP_AUTH_RUNKNOWN, LdapAuth3.login("notestuser@example.com", "topsecret", TEST_LDAP_PROPERTIES));
    }

    @Test
    public void testFailingLoginWithEmailAndWrongPassword() throws LDAPException {
        assertEquals(LDAP_AUTH_RUNKNOWN, LdapAuth3.login("testuser@example.com", "wrongpw", TEST_LDAP_PROPERTIES));
    }

    @Test
    public void testFailingLoginWithUsernameAndWrongPassword() throws LDAPException {
        assertEquals(LDAP_AUTH_RUNKNOWN, LdapAuth3.login("testuser", "wrongpw", TEST_LDAP_PROPERTIES));
    }

    @Test
    public void testFailingLoginWithoutC2Attr() throws LDAPException {
        assertEquals(LDAP_AUTH_RUNKNOWN, LdapAuth3.login("doe", "topsecret", TEST_LDAP_PROPERTIES));
    }

    @Test
    public void testFailingLoginWithoutBadStatus() throws LDAPException {
        assertEquals(LDAP_AUTH_RUNKNOWN, LdapAuth3.login("berserker", "topsecret", TEST_LDAP_PROPERTIES));
    }

    @Test
    public void testSecureLoginWithUsername() throws LDAPException {
        assertEquals(LDAP_AUTH_ROK, LdapAuth3.login("testuser", "topsecret", TEST_LDAPS_PROPERTIES));
    }
}

package de.ids_mannheim.korap.authentication;

import com.unboundid.ldap.listener.InMemoryDirectoryServer;
import com.unboundid.ldap.listener.InMemoryDirectoryServerConfig;
import com.unboundid.ldap.listener.InMemoryListenerConfig;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.util.Base64;
import com.unboundid.util.StaticUtils;
import com.unboundid.util.ssl.SSLUtil;
import com.unboundid.util.ssl.TrustAllTrustManager;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.security.GeneralSecurityException;

import static de.ids_mannheim.korap.authentication.LdapAuth3.LDAP_AUTH_ROK;
import static de.ids_mannheim.korap.authentication.LdapAuth3.LDAP_AUTH_RUNKNOWN;
import static org.junit.Assert.assertEquals;

public class LdapAuth3Test {
    InMemoryDirectoryServer server;

    @Before
    public void startDirectoryServer() throws LDAPException, GeneralSecurityException {
        InMemoryDirectoryServerConfig config = new InMemoryDirectoryServerConfig("dc=example,dc=com");
        config.addAdditionalBindCredentials("cn=admin,dc=example,dc=com", "adminpassword");
        config.setSchema(null);

        SSLUtil sslUtil = new SSLUtil(new TrustAllTrustManager());

        config.setListenerConfigs(InMemoryListenerConfig.createLDAPConfig("LDAP", // Listener name
                        null, // Listen address. (null = listen on all interfaces)
                        1234, // Listen port (0 = automatically choose an available port)
                        sslUtil.createSSLSocketFactory()), // StartTLS factory
                InMemoryListenerConfig.createLDAPSConfig("LDAPS", // Listener name
                        null, // Listen address. (null = listen on all interfaces)
                        1235, // Listen port (0 = automatically choose an available port)
                        sslUtil.createSSLServerSocketFactory(), // Server factory
                        sslUtil.createSSLSocketFactory())); // Client factory
        //       InMemoryDirectoryServer ds = new InMemoryDirectoryServer(config);
        server = new InMemoryDirectoryServer(config);

        // Start the server so it will accept client connections.
        server.startListening();
        String configPath = "src/test/resources/test-ldap-config.ldif";
        System.out.printf("Path %s\n", configPath);
        server.importFromLDIF(true, configPath);
        server.startListening();
    }

    @Test
    public void testLoginWithUsername() throws LDAPException {
        assertEquals(LDAP_AUTH_ROK, LdapAuth3.login("testuser", "topsecret", "src/test/resources/ldap.properties"));
    }


    @Test
    public void testLoginWithUid() throws LDAPException {
        final byte[] passwordBytes = StaticUtils.getBytes("password");
        String pw = Base64.encode(passwordBytes);
        assertEquals(LDAP_AUTH_ROK, LdapAuth3.login("testuser", pw, "src/test/resources/ldap.properties"));
    }

    @Test
    public void testLoginWithEmail() throws LDAPException {
        final byte[] passwordBytes = StaticUtils.getBytes("password");
        String pw = Base64.encode(passwordBytes);
        assertEquals(LDAP_AUTH_ROK, LdapAuth3.login("testuser@example.com", pw, "src/test/resources/ldap.properties"));
    }

    @Test
    public void testFailingLoginWithWrongEmail() throws LDAPException {
        assertEquals(LDAP_AUTH_RUNKNOWN, LdapAuth3.login("notestuser@example.com", "topsecret", "src/test/resources/ldap.properties"));
    }

    @Test
    public void testFailingLoginWithEmailAndWrongPassword() throws LDAPException {
        assertEquals(LDAP_AUTH_RUNKNOWN, LdapAuth3.login("testuser@example.com", "wrongpw", "src/test/resources/ldap.properties"));
    }

    @Test
    public void testFailingLoginWithUsernameAndWrongPassword() throws LDAPException {
        assertEquals(LDAP_AUTH_RUNKNOWN, LdapAuth3.login("testuser", "wrongpw", "src/test/resources/ldap.properties"));
    }

    @Test
    public void testFailingLoginWithoutC2Attr() throws LDAPException {
        assertEquals(LDAP_AUTH_RUNKNOWN, LdapAuth3.login("doe", "topsecret", "src/test/resources/ldap.properties"));
    }

    @Test
    public void testFailingLoginWithoutBadStatus() throws LDAPException {
        assertEquals(LDAP_AUTH_RUNKNOWN, LdapAuth3.login("berserker", "topsecret", "src/test/resources/ldap.properties"));
    }

    @After
    public void ShutDownDirectoryServer() {
        server.shutDown(true);
    }
}

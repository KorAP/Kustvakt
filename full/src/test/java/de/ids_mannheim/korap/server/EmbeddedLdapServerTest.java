package de.ids_mannheim.korap.server;

import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.util.Base64;
import com.unboundid.util.StaticUtils;
import de.ids_mannheim.korap.authentication.LdapAuth3;
import org.junit.AfterClass;
import org.junit.Test;

import java.net.UnknownHostException;
import java.security.GeneralSecurityException;

import static de.ids_mannheim.korap.authentication.LdapAuth3.LDAP_AUTH_RNAUTH;
import static de.ids_mannheim.korap.authentication.LdapAuth3.LDAP_AUTH_ROK;
import static org.junit.Assert.assertEquals;

public class EmbeddedLdapServerTest {

    public static final String EMBEDDED_LDAP_DEFAULT_CONF = "src/main/resources/embedded-ldap-default.conf";

    @AfterClass
    public static void shutdownEmbeddedLdapServer() {
        EmbeddedLdapServer.stop();
    }

    @Test
    public void embeddedServerStartsAutomaticallyAndUsersCanLogin() throws LDAPException {
        final byte[] passwordBytes = StaticUtils.getBytes("password");
        String pw = Base64.encode(passwordBytes);

        assertEquals(LDAP_AUTH_ROK, LdapAuth3.login("user", pw, EMBEDDED_LDAP_DEFAULT_CONF));
    }

    @Test
    public void usersWithUnencodedPasswowrdCanLogin() throws LDAPException {
        assertEquals(LDAP_AUTH_ROK, LdapAuth3.login("user1", "password1", EMBEDDED_LDAP_DEFAULT_CONF));
    }

    @Test
    public void asteriskPasswordsFail() throws LDAPException {
        assertEquals(LDAP_AUTH_RNAUTH, LdapAuth3.login("user1", "*", EMBEDDED_LDAP_DEFAULT_CONF));
    }

    @Test
    public void unauthorizedUsersAreNotAllowed() throws LDAPException {
        assertEquals(LDAP_AUTH_RNAUTH, LdapAuth3.login("yuser", "password", EMBEDDED_LDAP_DEFAULT_CONF));
    }

    @Test
    public void gettingMailForUser() throws LDAPException, UnknownHostException, GeneralSecurityException {
        EmbeddedLdapServer.startIfNotRunning(EMBEDDED_LDAP_DEFAULT_CONF);
        assertEquals("user2@example.com", LdapAuth3.getEmail("user2", EMBEDDED_LDAP_DEFAULT_CONF));
    }

    @Test
    public void gettingMailForUnknownUserIsNull() throws LDAPException, UnknownHostException, GeneralSecurityException {
        EmbeddedLdapServer.startIfNotRunning(EMBEDDED_LDAP_DEFAULT_CONF);
        assertEquals(null, LdapAuth3.getEmail("user1000", EMBEDDED_LDAP_DEFAULT_CONF));
    }
}

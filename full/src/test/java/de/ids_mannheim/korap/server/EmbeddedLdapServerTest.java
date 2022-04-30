package de.ids_mannheim.korap.server;

import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.util.Base64;
import com.unboundid.util.StaticUtils;
import de.ids_mannheim.korap.authentication.LdapAuth3;
import org.junit.AfterClass;
import org.junit.Test;

import static de.ids_mannheim.korap.authentication.LdapAuth3.LDAP_AUTH_RNAUTH;
import static de.ids_mannheim.korap.authentication.LdapAuth3.LDAP_AUTH_ROK;
import static org.junit.Assert.assertEquals;

public class EmbeddedLdapServerTest {

    @AfterClass
    public static void shutdownEmbeddedLdapServer() {
        EmbeddedLdapServer.stop();
    }

    @Test
    public void embeddedServerStartsAutomaticallyAndUsersCanLogin() throws LDAPException {
        final byte[] passwordBytes = StaticUtils.getBytes("password");
        String pw = Base64.encode(passwordBytes);

        assertEquals(LDAP_AUTH_ROK, LdapAuth3.login("user", pw, "src/main/resources/ldap.properties"));
    }

    @Test
    public void usersWithUnencodedPasswowrdCanLogin() throws LDAPException {
        assertEquals(LDAP_AUTH_ROK, LdapAuth3.login("user1", "password1", "src/main/resources/ldap.properties"));
    }

    @Test
    public void asteriskPasswordsFail() throws LDAPException {
        assertEquals(LDAP_AUTH_RNAUTH, LdapAuth3.login("user1", "*", "src/main/resources/ldap.properties"));
    }

    @Test
    public void unauthorizedUsersAreNotAllowed() throws LDAPException {
        assertEquals(LDAP_AUTH_RNAUTH, LdapAuth3.login("yuser", "password", "src/main/resources/ldap.properties"));
    }
}
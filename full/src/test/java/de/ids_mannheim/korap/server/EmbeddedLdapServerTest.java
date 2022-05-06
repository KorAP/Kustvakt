package de.ids_mannheim.korap.server;

import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.util.Base64;
import de.ids_mannheim.korap.authentication.LdapAuth3;
import org.junit.AfterClass;
import org.junit.Test;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;

import static de.ids_mannheim.korap.authentication.LdapAuth3.LDAP_AUTH_ROK;
import static de.ids_mannheim.korap.authentication.LdapAuth3.LDAP_AUTH_RUNKNOWN;
import static org.junit.Assert.assertEquals;

public class EmbeddedLdapServerTest {

    public static final String EMBEDDED_LDAP_DEFAULT_CONF = "src/main/resources/embedded-ldap-default.conf";

    @AfterClass
    public static void shutdownEmbeddedLdapServer() {
        EmbeddedLdapServer.stop();
    }

    @Test
    public void embeddedServerStartsAutomaticallyAndUsersCanLogin() throws LDAPException {
        assertEquals(LDAP_AUTH_ROK, LdapAuth3.login("user", "password", EMBEDDED_LDAP_DEFAULT_CONF));
    }

    @Test
    public void usersWithClearPasswordCanLogin() throws LDAPException {
        assertEquals(LDAP_AUTH_ROK, LdapAuth3.login("user1", "password1", EMBEDDED_LDAP_DEFAULT_CONF));
    }

    @Test
    public void usersWithSHA1PasswordCanLogin() throws LDAPException, NoSuchAlgorithmException {
        assertEquals(LDAP_AUTH_ROK, LdapAuth3.login("user3", "password3", EMBEDDED_LDAP_DEFAULT_CONF));
    }

    @Test
    public void usersWithSHA256PasswordCanLogin() throws LDAPException, NoSuchAlgorithmException, InvalidKeySpecException {
        assertEquals(LDAP_AUTH_ROK, LdapAuth3.login("user4", "password4", EMBEDDED_LDAP_DEFAULT_CONF));
    }

    @Test
    public void asteriskPasswordsFail() throws LDAPException {
        assertEquals(LDAP_AUTH_RUNKNOWN, LdapAuth3.login("user1", "*", EMBEDDED_LDAP_DEFAULT_CONF));
    }

    @Test
    public void loginWithPreencodedPBKDF2Password() throws LDAPException, NoSuchAlgorithmException, InvalidKeySpecException {
        byte[] salt = new byte[32];
        KeySpec spec = new PBEKeySpec("password5".toCharArray(), salt, 65536, 256);
        SecretKeyFactory f = SecretKeyFactory.getInstance("PBKDF2withHmacSHA256");
        byte[] hash = f.generateSecret(spec).getEncoded();

        final String pbkdf2sha256Password = "{PBKDF2-SHA256}" + Base64.encode(hash);
        System.out.println(pbkdf2sha256Password);
        assertEquals(LDAP_AUTH_ROK, LdapAuth3.login("user5", pbkdf2sha256Password, EMBEDDED_LDAP_DEFAULT_CONF));
    }

    @Test
    public void loginWithUnEncodedPBKDF2PasswordFails() throws LDAPException, NoSuchAlgorithmException, InvalidKeySpecException {
        assertEquals(LDAP_AUTH_RUNKNOWN, LdapAuth3.login("user5", "password5", EMBEDDED_LDAP_DEFAULT_CONF));
    }

    @Test
    public void unauthorizedUsersAreNotAllowed() throws LDAPException {
        assertEquals(LDAP_AUTH_RUNKNOWN, LdapAuth3.login("yuser", "password", EMBEDDED_LDAP_DEFAULT_CONF));
    }

    @Test
    public void gettingMailForUser() throws LDAPException {
        assertEquals("user2@example.com", LdapAuth3.getEMailFromUid("user2", EMBEDDED_LDAP_DEFAULT_CONF));
    }

    @Test
    public void gettingMailForUnknownUserIsNull() throws LDAPException {
        assertEquals(null, LdapAuth3.getEMailFromUid("user1000", EMBEDDED_LDAP_DEFAULT_CONF));
    }


}

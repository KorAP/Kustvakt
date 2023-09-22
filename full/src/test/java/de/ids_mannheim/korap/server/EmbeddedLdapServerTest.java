package de.ids_mannheim.korap.server;

import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.util.Base64;
import de.ids_mannheim.korap.authentication.LdapAuth3;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.net.UnknownHostException;
import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;

import static de.ids_mannheim.korap.authentication.LdapAuth3.LDAP_AUTH_ROK;
import static de.ids_mannheim.korap.authentication.LdapAuth3.LDAP_AUTH_RUNKNOWN;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;

@DisplayName("Embedded Ldap Server Test")
class EmbeddedLdapServerTest {

    public static final String TEST_EMBEDDED_LDAP_CONF = "src/test/resources/test-embedded-ldap.conf";

    @AfterAll
    static void shutdownEmbeddedLdapServer() {
        EmbeddedLdapServer.stop();
    }

    @Test
    @DisplayName("Embedded Server Starts Automatically And Users Can Login")
    void embeddedServerStartsAutomaticallyAndUsersCanLogin() throws LDAPException {
        assertEquals(LDAP_AUTH_ROK, LdapAuth3.login("user", "password", TEST_EMBEDDED_LDAP_CONF));
    }

    @Test
    @DisplayName("Users With Clear Password Can Login")
    void usersWithClearPasswordCanLogin() throws LDAPException {
        assertEquals(LDAP_AUTH_ROK, LdapAuth3.login("user1", "password1", TEST_EMBEDDED_LDAP_CONF));
    }

    @Test
    @DisplayName("Users With SHA 1 Password Can Login")
    void usersWithSHA1PasswordCanLogin() throws LDAPException, NoSuchAlgorithmException {
        assertEquals(LDAP_AUTH_ROK, LdapAuth3.login("user3", "password3", TEST_EMBEDDED_LDAP_CONF));
    }

    @Test
    @DisplayName("Users With SHA 256 Password Can Login")
    void usersWithSHA256PasswordCanLogin() throws LDAPException, NoSuchAlgorithmException, InvalidKeySpecException {
        assertEquals(LDAP_AUTH_ROK, LdapAuth3.login("user4", "password4", TEST_EMBEDDED_LDAP_CONF));
    }

    @Test
    @DisplayName("Asterisk Passwords Fail")
    void asteriskPasswordsFail() throws LDAPException {
        assertEquals(LDAP_AUTH_RUNKNOWN, LdapAuth3.login("user1", "*", TEST_EMBEDDED_LDAP_CONF));
    }

    @Test
    @DisplayName("Login With Preencoded PBKDF 2 Password")
    void loginWithPreencodedPBKDF2Password() throws LDAPException, NoSuchAlgorithmException, InvalidKeySpecException {
        byte[] salt = new byte[32];
        KeySpec spec = new PBEKeySpec("password5".toCharArray(), salt, 65536, 256);
        SecretKeyFactory f = SecretKeyFactory.getInstance("PBKDF2withHmacSHA256");
        byte[] hash = f.generateSecret(spec).getEncoded();
        final String pbkdf2sha256Password = "{PBKDF2-SHA256}" + Base64.encode(hash);
        // System.out.println(pbkdf2sha256Password);
        assertEquals(LDAP_AUTH_ROK, LdapAuth3.login("user5", pbkdf2sha256Password, TEST_EMBEDDED_LDAP_CONF));
    }

    @Test
    @DisplayName("Login With Un Encoded PBKDF 2 Password Fails")
    void loginWithUnEncodedPBKDF2PasswordFails() throws LDAPException, NoSuchAlgorithmException, InvalidKeySpecException {
        assertEquals(LDAP_AUTH_RUNKNOWN, LdapAuth3.login("user5", "password5", TEST_EMBEDDED_LDAP_CONF));
    }

    @Test
    @DisplayName("Unauthorized Users Are Not Allowed")
    void unauthorizedUsersAreNotAllowed() throws LDAPException {
        assertEquals(LDAP_AUTH_RUNKNOWN, LdapAuth3.login("yuser", "password", TEST_EMBEDDED_LDAP_CONF));
    }

    @Test
    @DisplayName("Getting Mail For User")
    void gettingMailForUser() throws LDAPException, UnknownHostException, GeneralSecurityException {
        EmbeddedLdapServer.startIfNotRunning(TEST_EMBEDDED_LDAP_CONF);
        assertEquals(LdapAuth3.getEmail("user2", TEST_EMBEDDED_LDAP_CONF), "user2@example.com");
    }

    @Test
    @DisplayName("Getting Mail Fo RNAUTH User Is Null")
    void gettingMailFoRNAUTHUserIsNull() throws LDAPException, UnknownHostException, GeneralSecurityException {
        EmbeddedLdapServer.startIfNotRunning(TEST_EMBEDDED_LDAP_CONF);
        assertEquals(null, LdapAuth3.getEmail("user1000", TEST_EMBEDDED_LDAP_CONF));
    }
}

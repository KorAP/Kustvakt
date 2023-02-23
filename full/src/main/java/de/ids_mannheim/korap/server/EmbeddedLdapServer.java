package de.ids_mannheim.korap.server;

import com.unboundid.ldap.listener.*;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.util.CryptoHelper;
import com.unboundid.util.ssl.KeyStoreKeyManager;
import com.unboundid.util.ssl.SSLUtil;
import com.unboundid.util.ssl.TrustAllTrustManager;
import com.unboundid.util.ssl.TrustStoreTrustManager;
import de.ids_mannheim.korap.authentication.LDAPConfig;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;

public class EmbeddedLdapServer {
    static InMemoryDirectoryServer server;

    public static void startIfNotRunning(LDAPConfig ldapConfig) throws LDAPException, GeneralSecurityException, UnknownHostException {
        if (server != null) {
            return;
        }

        InMemoryDirectoryServerConfig config = new InMemoryDirectoryServerConfig(ldapConfig.searchBase);
        final MessageDigest sha1Digest = CryptoHelper.getMessageDigest("SHA1");
        final MessageDigest sha256Digest = CryptoHelper.getMessageDigest("SHA-256");
        config.setPasswordEncoders(new ClearInMemoryPasswordEncoder("{CLEAR}", null), new ClearInMemoryPasswordEncoder("{HEX}", HexPasswordEncoderOutputFormatter.getLowercaseInstance()), new ClearInMemoryPasswordEncoder("{BASE64}", Base64PasswordEncoderOutputFormatter.getInstance()), new UnsaltedMessageDigestInMemoryPasswordEncoder("{SHA}", Base64PasswordEncoderOutputFormatter.getInstance(), sha1Digest), new UnsaltedMessageDigestInMemoryPasswordEncoder("{SHA256}", Base64PasswordEncoderOutputFormatter.getInstance(), sha256Digest));
        config.addAdditionalBindCredentials(ldapConfig.sLoginDN, ldapConfig.sPwd);
        config.setSchema(null);

        final SSLUtil clientSslUtil = new SSLUtil(new TrustAllTrustManager());

        if (ldapConfig.useSSL) {
            // As explained here (by Neil Wilson from the UnboundId team):
            // http://stackoverflow.com/questions/19713967/adding-an-ssl-listener-to-unboundid
            final SSLUtil serverSSLUtil = new SSLUtil(new KeyStoreKeyManager(ldapConfig.keyStorePath, ldapConfig.keyStorePassword.toCharArray(), "JKS", "localhost"), new TrustStoreTrustManager(ldapConfig.trustStorePath));

            config.setListenerConfigs(InMemoryListenerConfig.createLDAPSConfig("LDAPS",
                    (ldapConfig.listenHost != null ? InetAddress.getByName(ldapConfig.listenHost) : null),
                    ldapConfig.port, serverSSLUtil.createSSLServerSocketFactory(), clientSslUtil.createSSLSocketFactory()));
        } else {
            config.setListenerConfigs(InMemoryListenerConfig.createLDAPConfig("LDAP", // Listener name
                    (ldapConfig.listenHost != null ? InetAddress.getByName(ldapConfig.listenHost) : null),
                    ldapConfig.port, // Listen port (0 = automatically choose an available port)
                    clientSslUtil.createSSLSocketFactory()));
        }
        server = new InMemoryDirectoryServer(config);

        server.importFromLDIF(true, ldapConfig.ldif);
        server.startListening();
    }

    public static void startIfNotRunning(String ldapConfigFilename) throws LDAPException, GeneralSecurityException, UnknownHostException {
        LDAPConfig ldapConfig = new LDAPConfig(ldapConfigFilename);
        startIfNotRunning(ldapConfig);
    }

    public static void stop() {
        if (server != null) {
            server.shutDown(true);
            server = null;
        }
    }

}

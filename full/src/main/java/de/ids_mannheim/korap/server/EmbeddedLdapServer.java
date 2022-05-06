package de.ids_mannheim.korap.server;

import com.unboundid.ldap.listener.*;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.util.CryptoHelper;
import com.unboundid.util.ssl.SSLUtil;
import com.unboundid.util.ssl.TrustAllTrustManager;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class EmbeddedLdapServer {
    public static InMemoryDirectoryServer server;

    public static void start(String ldapConfigFilename) throws LDAPException, GeneralSecurityException, UnknownHostException {
        Map<String, String> ldapConfig = null;
        try {
            ldapConfig = loadProp(ldapConfigFilename);
        } catch (IOException e) {
            System.out.println("Error: LDAPAuth.login: cannot load Property file!");
        }

        final int ldapPort = Integer.parseInt(ldapConfig.getOrDefault("ldapPort", "3268"));
        final String ldapBase = ldapConfig.getOrDefault("ldapBase", "dc=example,dc=com");
        final String sLoginDN = ldapConfig.getOrDefault("sLoginDN", "cn=admin,dc=example,dc=com");
        final String sPwd = ldapConfig.getOrDefault("pwd", "");
        final String ldif = ldapConfig.getOrDefault("ldifFile", "");

        InMemoryDirectoryServerConfig config = new InMemoryDirectoryServerConfig(ldapBase);
        final MessageDigest sha1Digest = CryptoHelper.getMessageDigest("SHA1");
        final MessageDigest sha256Digest = CryptoHelper.getMessageDigest("SHA-256");
        config.setPasswordEncoders(new ClearInMemoryPasswordEncoder("{CLEAR}", null), new ClearInMemoryPasswordEncoder("{HEX}", HexPasswordEncoderOutputFormatter.getLowercaseInstance()), new ClearInMemoryPasswordEncoder("{BASE64}", Base64PasswordEncoderOutputFormatter.getInstance()), new UnsaltedMessageDigestInMemoryPasswordEncoder("{SHA}", Base64PasswordEncoderOutputFormatter.getInstance(), sha1Digest), new UnsaltedMessageDigestInMemoryPasswordEncoder("{SHA256}", Base64PasswordEncoderOutputFormatter.getInstance(), sha256Digest));
        config.addAdditionalBindCredentials(sLoginDN, sPwd);
        config.setSchema(null);

        final SSLUtil clientSslUtil = new SSLUtil(new TrustAllTrustManager());

        config.setListenerConfigs(InMemoryListenerConfig.createLDAPConfig("LDAP", // Listener name
                InetAddress.getByName("localhost"), // Listen address. (null = listen on all interfaces)
                ldapPort, // Listen port (0 = automatically choose an available port)
                clientSslUtil.createSSLSocketFactory()));
        server = new InMemoryDirectoryServer(config);

        server.importFromLDIF(true, ldif);
        server.startListening();
    }

    public static void stop() {
        if (server != null) {
            server.shutDown(true);
        }
    }

    static HashMap<String, String> typeCastConvert(Properties prop) {
        Map<String, String> step2 = (Map<String, String>) (Map) prop;
        return new HashMap<>(step2);
    }

    public static HashMap<String, String> loadProp(String sConfFile) throws IOException {
        FileInputStream in;
        Properties prop;

        try {
            in = new FileInputStream(sConfFile);
        } catch (IOException ex) {
            System.err.printf("Error: LDAP.loadProp: cannot load Property file '%s'!\n", sConfFile);
            ex.printStackTrace();
            return null;
        }

        prop = new Properties();

        try {
            prop.load(in);
            return typeCastConvert(prop);
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        return new HashMap<>();
    }

}

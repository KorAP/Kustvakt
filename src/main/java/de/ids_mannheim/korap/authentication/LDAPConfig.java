package de.ids_mannheim.korap.authentication;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/*
 * adding ldapTimeout for controlling timeout handling during LDAP operations - 23.06.25/FB
 */

public class LDAPConfig {
    public final boolean useSSL;
    public final String host;
    public final int port;
    public final String searchBase;
    public final String sLoginDN;
    public final String searchFilter;
    public final String sPwd;
    public final String trustStorePath;
    public final String additionalCipherSuites;
    public final boolean useEmbeddedServer;
    public final String emailAttribute;
    public final String ldif;
    public final String authFilter;
    public final String userNotBlockedFilter;
    public final int 	ldapTimeout; // sets LDAP operation timeout [ms]. should probably be < 10s (= network timeout).
    
    // default timeout for LDAP operations in [ms]. Should be < than default network timeout of 10s:
    public static final String LDAP_DEFAULT_TIMEOUT = "9000"; 
    
    public LDAPConfig (String ldapConfigFilename)
            throws LdapConfigurationException {
        Map<String, String> ldapConfig = null;
        try {
            ldapConfig = loadProp(ldapConfigFilename);
        }
        catch (IOException e) {
            System.out.println(
                    "Error: LDAPAuth.login: cannot load Property file!");
        }

        useSSL = Boolean
                .parseBoolean(ldapConfig.getOrDefault("useSSL", "false"));
        host = ldapConfig.getOrDefault("host", "localhost");
        port = Integer.parseInt(
                ldapConfig.getOrDefault("port", (useSSL ? "636" : "389")));
        searchBase = getConfigOrThrow(ldapConfig, "searchBase");
        sLoginDN = getConfigOrThrow(ldapConfig, "sLoginDN");
        searchFilter = getConfigOrThrow(ldapConfig, "searchFilter");
        authFilter = ldapConfig.getOrDefault("authFilter", null);
        userNotBlockedFilter = ldapConfig.getOrDefault("userNotBlockedFilter",
                null);
        sPwd = ldapConfig.getOrDefault("pwd", "");
        trustStorePath = ldapConfig.getOrDefault("trustStore", "");
        additionalCipherSuites = ldapConfig
                .getOrDefault("additionalCipherSuites", "");
        useEmbeddedServer = Boolean.parseBoolean(
                ldapConfig.getOrDefault("useEmbeddedServer", "false"));
        emailAttribute = ldapConfig.getOrDefault("emailAttribute", "mail");
        ldif = ldapConfig.getOrDefault("ldifFile", null);
        ldapTimeout = Integer.parseInt(ldapConfig.getOrDefault("ldapTimeout",  LDAP_DEFAULT_TIMEOUT)); 
    }

    static HashMap<String, String> typeCastConvert (Properties prop) {
        Map<String, String> step2 = (Map<String, String>) (Map) prop;
        return new HashMap<>(step2);
    }

    public static HashMap<String, String> loadProp (String sConfFile)
            throws IOException {
        FileInputStream in;
        Properties prop;

        try {
            in = new FileInputStream(sConfFile);
        }
        catch (IOException ex) {
            System.err.printf(
                    "Error: LDAP.loadProp: cannot load Property file '%s'!\n",
                    sConfFile);
            ex.printStackTrace();
            return null;
        }

        prop = new Properties();

        try {
            prop.load(in);
            return typeCastConvert(prop);
        }
        catch (IOException ex) {
            ex.printStackTrace();
        }

        return new HashMap<>();
    }

    private String getConfigOrThrow (Map<String, String> ldapConfig,
            String attribute) throws LdapConfigurationException {
        String value = ldapConfig.get(attribute);
        if (value != null)
            return value;
        else
            throw new LdapConfigurationException(attribute + " is not set");
    }

    private class LdapConfigurationException extends RuntimeException {
        public LdapConfigurationException (String s) {
            super(s);
        }
    }

}

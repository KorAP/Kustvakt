/*
 *   user authentication via LDAP
 */

package de.ids_mannheim.korap.authentication;

import com.nimbusds.jose.JOSEException;
import com.unboundid.ldap.sdk.*;
import com.unboundid.util.ssl.SSLUtil;
import com.unboundid.util.ssl.TrustAllTrustManager;
import com.unboundid.util.ssl.TrustStoreTrustManager;
import de.ids_mannheim.korap.config.FullConfiguration;
import de.ids_mannheim.korap.constant.TokenType;
import de.ids_mannheim.korap.server.EmbeddedLdapServer;
import org.apache.commons.text.StringSubstitutor;

import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.net.UnknownHostException;
import java.security.GeneralSecurityException;
import java.util.*;

import static de.ids_mannheim.korap.server.EmbeddedLdapServer.loadProp;


/**
 * LDAP Login
 *
 * @author bodmer, margaretha, kupietz
 * @see APIAuthentication
 */
public class LdapAuth3 extends APIAuthentication {

    public static final int LDAP_AUTH_ROK = 0;
    public static final int LDAP_AUTH_RCONNECT = 1; // cannot connect to LDAP Server
    public static final int LDAP_AUTH_RINTERR = 2; // internal error: cannot verify User+Pwd.
    public static final int LDAP_AUTH_RUNKNOWN = 3; // User Account or Pwd unknown;
    final static Boolean DEBUGLOG = false;

    public LdapAuth3(FullConfiguration config) throws JOSEException {
        super(config);
    }

    public static String getErrMessage(int code) {
        switch (code) {
            case LDAP_AUTH_ROK:
                return "LDAP Authentication successful.";
            case LDAP_AUTH_RCONNECT:
                return "LDAP Authentication: connecting to LDAP Server failed!";
            case LDAP_AUTH_RINTERR:
                return "LDAP Authentication failed due to an internal error!";
            case LDAP_AUTH_RUNKNOWN:
                return "LDAP Authentication failed due to unknown user or password!";
            default:
                return "LDAP Authentication failed with unknown error code!";
        }
    }

    public static int login(String sUserDN, String sUserPwd, String ldapConfigFilename) throws LDAPException {

        sUserDN = Filter.encodeValue(sUserDN);
        sUserPwd = Filter.encodeValue(sUserPwd);
        LDAPConfig ldapConfig = new LDAPConfig(ldapConfigFilename);

        SearchResult srchRes = search(sUserDN, sUserPwd, ldapConfig, !ldapConfig.ldapFilter.contains("${password}"));

        if (srchRes == null || srchRes.getEntryCount() == 0) {
            if (DEBUGLOG) System.out.printf("Finding '%s': no entry found!\n", sUserDN);
            return LDAP_AUTH_RUNKNOWN;
        }

        return LDAP_AUTH_ROK;
    }

    public static SearchResult search(String sUserDN, String sUserPwd, LDAPConfig ldapConfig, boolean bindWithFoundDN) throws LDAPException {
        Map<String, String> valuesMap = new HashMap<>();
        valuesMap.put("username", sUserDN);
        valuesMap.put("password", sUserPwd);
        StringSubstitutor sub = new StringSubstitutor(valuesMap);
        String ldapFilterInstance = sub.replace(ldapConfig.ldapFilter);

        if (DEBUGLOG) {
            //System.out.printf("LDAP Version      = %d.\n", LDAPConnection.LDAP_V3);
            System.out.printf("LDAP Host & Port  = '%s':%d.\n", ldapConfig.ldapHost, ldapConfig.ldapPort);
            System.out.printf("Login User = '%s'\n", sUserDN);
        }

        // LDAP Connection:
        if (DEBUGLOG) System.out.println("LDAPS " + ldapConfig.ldapS);

        LDAPConnection lc;

        if (ldapConfig.ldapS) {
            try {
                SSLUtil sslUtil;
                if (ldapConfig.trustStorePath != null && !ldapConfig.trustStorePath.isEmpty()) {
                    sslUtil = new SSLUtil(new TrustStoreTrustManager(ldapConfig.trustStorePath));
                } else {
                    sslUtil = new SSLUtil(new TrustAllTrustManager());
                }
                if (ldapConfig.additionalCipherSuites != null && !ldapConfig.additionalCipherSuites.isEmpty()) {
                    addSSLCipherSuites(ldapConfig.additionalCipherSuites);
                }
                SSLSocketFactory socketFactory = sslUtil.createSSLSocketFactory();
                lc = new LDAPConnection(socketFactory);
            } catch (GeneralSecurityException e) {
                System.err.printf("Error: login: Connecting to LDAPS Server: failed: '%s'!\n", e);
                ldapTerminate(null);
                return null;
            }
        } else {
            lc = new LDAPConnection();
        }
        try {
            lc.connect(ldapConfig.ldapHost, ldapConfig.ldapPort);
            if (DEBUGLOG && ldapConfig.ldapS) System.out.println("LDAPS Connection = OK\n");
            if (DEBUGLOG && !ldapConfig.ldapS) System.out.println("LDAP Connection = OK\n");
        } catch (LDAPException e) {
            String fullStackTrace = org.apache.commons.lang.exception.ExceptionUtils.getFullStackTrace(e);
            System.err.printf("Error: login: Connecting to LDAP Server: failed: '%s'!\n", fullStackTrace);
            ldapTerminate(lc);
            return null;
        }


        if (DEBUGLOG) System.out.printf("Debug: isConnected=%d\n", lc.isConnected() ? 1 : 0);

        try {
            // bind to server:
            if (DEBUGLOG) System.out.printf("Binding with '%s' ...\n", ldapConfig.sLoginDN);
            lc.bind(ldapConfig.sLoginDN, ldapConfig.sPwd);
            if (DEBUGLOG) System.out.print("Binding: OK.\n");
        } catch (LDAPException e) {
            System.err.printf("Error: login: Binding failed: '%s'!\n", e);
            ldapTerminate(lc);
            return null;
        }

        if (DEBUGLOG) System.out.printf("Debug: isConnected=%d\n", lc.isConnected() ? 1 : 0);

        if (DEBUGLOG) System.out.printf("Finding user '%s'...\n", sUserDN);

        SearchResult srchRes;
        try {
            // SCOPE_SUB = Scope Subtree.
            if (DEBUGLOG) System.out.printf("Finding Filter: '%s'.\n", ldapFilterInstance);

            srchRes = lc.search(ldapConfig.ldapBase, SearchScope.SUB, ldapFilterInstance);

            if (DEBUGLOG) System.out.printf("Finding '%s': %d entries.\n", sUserDN, srchRes.getEntryCount());
        } catch (LDAPSearchException e) {
            System.err.printf("Error: Search for User failed: '%s'!\n", e);
            ldapTerminate(lc);
            return null;
        }

        if (srchRes == null || srchRes.getEntryCount() == 0) {
            if (DEBUGLOG) System.out.printf("Finding '%s': no entry found!\n", sUserDN);
            ldapTerminate(lc);
            return null;
        }

        if (bindWithFoundDN) {
            String matchedDN = srchRes.getSearchEntries().get(0).getDN();
            if (DEBUGLOG) System.out.printf("Requested bind for found user %s' failed.\n", matchedDN);
            try {
                // bind to server:
                if (DEBUGLOG) System.out.printf("Binding with '%s' ...\n", matchedDN);
                lc.bind(matchedDN, sUserPwd);
                if (DEBUGLOG) System.out.print("Binding: OK.\n");
            } catch (LDAPException e) {
                System.err.printf("Error: login: Binding failed: '%s'!\n", e);
                ldapTerminate(lc);
                return null;
            }
        }

        ldapTerminate(lc);
        return srchRes;
    }

    public static String getEMailFromUid(String sUserDN, String ldapConfigFilename) throws LDAPException {
        String sUserPwd = "*";
        LDAPConfig ldapConfig = new LDAPConfig(ldapConfigFilename);
        final String emailAttribute = ldapConfig.emailAttribute;

        SearchResult searchResult = search(sUserDN, sUserPwd, ldapConfig, false);

        if (searchResult == null) {
            return null;
        }

        for (SearchResultEntry entry : searchResult.getSearchEntries()) {
            String mail = entry.getAttributeValue(emailAttribute);
            if (mail != null) {
                return mail;
            }
        }
        return null;
    }

    public static void ldapTerminate(LDAPConnection lc) {
        if (DEBUGLOG) System.out.println("Terminating...");

        if (lc != null) {
            lc.close(null);
        }
        if (DEBUGLOG) System.out.println("closing connection: done.\n");
    }

    private static void addSSLCipherSuites(String ciphersCsv) {
        // add e.g. TLS_RSA_WITH_AES_256_GCM_SHA384
        Set<String> ciphers = new HashSet<>();
        ciphers.addAll(SSLUtil.getEnabledSSLCipherSuites());
        ciphers.addAll(Arrays.asList(ciphersCsv.split(", *")));
        SSLUtil.setEnabledSSLCipherSuites(ciphers);
    }

    @Override
    public TokenType getTokenType() {
        return TokenType.API;
    }

    static class LDAPConfig {
        public final boolean ldapS;
        public final String ldapHost;
        public final int ldapPort;
        final String ldapBase;
        final String sLoginDN;
        final String ldapFilter;
        final String sPwd;
        final String trustStorePath;
        final String additionalCipherSuites;
        final boolean useEmbeddedServer;
        private final String emailAttribute;

        public LDAPConfig(String ldapConfigFilename) throws LdapConfigurationException {
            Map<String, String> ldapConfig = null;
            try {
                ldapConfig = loadProp(ldapConfigFilename);
            } catch (IOException e) {
                System.out.println("Error: LDAPAuth.login: cannot load Property file!");
            }

            ldapS = Boolean.parseBoolean(ldapConfig.getOrDefault("ldapS", "false"));
            ldapHost = ldapConfig.getOrDefault("ldapHost", "localhost");
            ldapPort = Integer.parseInt(ldapConfig.getOrDefault("ldapPort", (ldapS ? "636" : "389")));
            ldapBase = getConfigOrThrow(ldapConfig, "ldapBase");
            sLoginDN = getConfigOrThrow(ldapConfig, "sLoginDN");
            ldapFilter = getConfigOrThrow(ldapConfig, "ldapFilter");
            sPwd = ldapConfig.getOrDefault("pwd", "");
            trustStorePath = ldapConfig.getOrDefault("trustStore", "");
            additionalCipherSuites = ldapConfig.getOrDefault("additionalCipherSuites", "");
            useEmbeddedServer = Boolean.parseBoolean(ldapConfig.getOrDefault("useEmbeddedServer", "false"));
            emailAttribute = ldapConfig.getOrDefault("emailAttribute", "mail");

            if (useEmbeddedServer && EmbeddedLdapServer.server == null) {
                try {
                    EmbeddedLdapServer.start(ldapConfigFilename);
                } catch (GeneralSecurityException | UnknownHostException | LDAPException e) {
                    throw new RuntimeException(e);
                }
            }


        }

        private String getConfigOrThrow(Map<String, String> ldapConfig, String attribute) throws LdapConfigurationException {
            String value = ldapConfig.get(attribute);
            if (value != null) return value;
            else throw new LdapConfigurationException(attribute + " is not set");
        }

        private class LdapConfigurationException extends RuntimeException {
            public LdapConfigurationException(String s) {
                super(s);
            }
        }
    }
}

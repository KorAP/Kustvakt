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
import java.net.UnknownHostException;
import java.security.GeneralSecurityException;
import java.util.*;


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
    /* cannot be distinguished, currently
    public static final int LDAP_AUTH_RUNKNOWN = 3; // User Account or Pwd unknown;
    public static final int LDAP_AUTH_RLOCKED = 4; // User Account locked;
    public static final int LDAP_AUTH_RNOTREG = 5; // User known, but has not registered to KorAP/C2 Service yet;
     */
    public static final int LDAP_AUTH_RNOEMAIL = 6; // cannot obtain email for sUserDN
    public static final int LDAP_AUTH_RNAUTH = 7; // User Account or Pwd unknown, or not authorized
    final static Boolean DEBUGLOG = false;        // log debug output.

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
/* cannot be distinguished, currently
            case LDAP_AUTH_RUNKNOWN:
                return "LDAP Authentication failed due to unknown user or password!";
            case LDAP_AUTH_RLOCKED:
                return "LDAP Authentication: known user is locked!";
            case LDAP_AUTH_RNOTREG:
                return "LDAP Authentication: known user has not registered yet!";
*/
            case LDAP_AUTH_RNOEMAIL:
                return "LDAP Authentication: known user, but cannot obtain email!";
            case LDAP_AUTH_RNAUTH:
                return "LDAP Authentication: unknown user or password, or user is locked or not authorized!";
            default:
                return "LDAP Authentication failed with unknown error code!";
        }
    }

    public static int login(String login, String password, String ldapConfigFilename) throws LDAPException {
        LDAPConfig ldapConfig = new LDAPConfig(ldapConfigFilename);

        login = Filter.encodeValue(login);
        password = Filter.encodeValue(password);

        if (ldapConfig.useEmbeddedServer) {
            try {
                EmbeddedLdapServer.startIfNotRunning(ldapConfig);
            } catch (GeneralSecurityException | UnknownHostException | LDAPException e) {
                throw new RuntimeException(e);
            }
        }

        SearchResult srchRes = search(login, password, ldapConfig);

        if (srchRes == null || srchRes.getEntryCount() == 0) {
            if (DEBUGLOG) System.out.printf("Finding '%s': no entry found!\n", login);
            return LDAP_AUTH_RNAUTH;
        }
        return LDAP_AUTH_ROK;
    }

    public static SearchResult search(String login, String password, LDAPConfig ldapConfig) throws LDAPException {
        Map<String, String> valuesMap = new HashMap<>();
        valuesMap.put("login", login);
        valuesMap.put("password", password);
        StringSubstitutor sub = new StringSubstitutor(valuesMap);
        String searchFilterInstance = sub.replace(ldapConfig.searchFilter);

        valuesMap.clear();
        valuesMap.put("login", login);
        sub = new StringSubstitutor(valuesMap);
        String insensitiveSearchFilter = sub.replace(ldapConfig.searchFilter);

        if (DEBUGLOG) {
            //System.out.printf("LDAP Version      = %d.\n", LDAPConnection.LDAP_V3);
            System.out.printf("LDAP Host & Port  = '%s':%d.\n", ldapConfig.host, ldapConfig.port);
            System.out.printf("Login User = '%s'\n", login);
        }

        // LDAP Connection:
        if (DEBUGLOG) System.out.println("LDAPS " + ldapConfig.useSSL);

        LDAPConnection lc;

        if (ldapConfig.useSSL) {
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
            lc.connect(ldapConfig.host, ldapConfig.port);
            if (DEBUGLOG && ldapConfig.useSSL) System.out.println("LDAPS Connection = OK\n");
            if (DEBUGLOG && !ldapConfig.useSSL) System.out.println("LDAP Connection = OK\n");
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

        if (DEBUGLOG) System.out.printf("Finding user '%s'...\n", login);

        SearchResult srchRes;
        try {
            // SCOPE_SUB = Scope Subtree.
            if (DEBUGLOG) System.out.printf("Finding Filter: '%s'.\n", insensitiveSearchFilter);

            srchRes = lc.search(ldapConfig.searchBase, SearchScope.SUB, searchFilterInstance);

            if (DEBUGLOG) System.out.printf("Finding '%s': %d entries.\n", login, srchRes.getEntryCount());
        } catch (LDAPSearchException e) {
            System.err.printf("Error: login: Search for User failed: '%s'!\n", e);
            ldapTerminate(lc);
            return null;
        }

        if (srchRes.getEntryCount() == 0) {
            if (DEBUGLOG) System.out.printf("Finding '%s': no entry found!\n", login);
            return null;
        }

        ldapTerminate(lc);
        return srchRes;
    }

    public static String getEmail(String sUserDN, String ldapConfigFilename) throws LDAPException {
        String sUserPwd = "*";
        LDAPConfig ldapConfig = new LDAPConfig(ldapConfigFilename);
        final String emailAttribute = ldapConfig.emailAttribute;

        SearchResult searchResult = search(sUserDN, sUserPwd, ldapConfig);

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

}

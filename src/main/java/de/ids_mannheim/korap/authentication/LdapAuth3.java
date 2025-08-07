/*
 *   user authentication via LDAP
 */

package de.ids_mannheim.korap.authentication;

import java.net.UnknownHostException;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.SSLSocketFactory;

import org.apache.commons.text.StringSubstitutor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.unboundid.ldap.sdk.BindResult;
import com.unboundid.ldap.sdk.Filter;
import com.unboundid.ldap.sdk.LDAPConnection;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.LDAPSearchException;
import com.unboundid.ldap.sdk.ResultCode;
import com.unboundid.ldap.sdk.SearchResult;
import com.unboundid.ldap.sdk.SearchResultEntry;
import com.unboundid.ldap.sdk.SearchScope;
import com.unboundid.util.NotNull;
import com.unboundid.util.ssl.SSLUtil;
import com.unboundid.util.ssl.TrustAllTrustManager;
import com.unboundid.util.ssl.TrustStoreTrustManager;

import de.ids_mannheim.korap.server.EmbeddedLdapServer;

/**
 * LDAP Login
 *
 * @author bodmer, margaretha, kupietz
 */
public class LdapAuth3 {

	// return codes:
    public static final int LDAP_AUTH_ROK 		= 0;
    public static final int LDAP_AUTH_RCONNECT 	= 1; 	// cannot connect to LDAP Server
    public static final int LDAP_AUTH_RINTERR 	= 2; 	// internal error: cannot verify User+Pwd.
    public static final int LDAP_AUTH_RUNKNOWN 	= 3; 	// User Account or Pwd unknown;
    public static final int LDAP_AUTH_RLOCKED 	= 4; 	// User Account locked;
    public static final int LDAP_AUTH_RNOTREG 	= 5; 	// User known, but has not registered to KorAP/C2 Service yet;
    public static final int LDAP_AUTH_RNOEMAIL 	= 6; 	// cannot obtain email for sUserDN
    public static final int LDAP_AUTH_RNAUTH 	= 7; 	// User Account or Pwd unknown, or not authorized
    public static final int LDAP_AUTH_RTIMEOUT 	= 100; 	// could not reach LDAP server due to timeout (connect, search).
    
    // other constants:
    private static final String PATT_TIMEOUT_MESS  = "Unable to establish a connection.* within the configured timeout";
    													// pattern of message returned by the Cause() of a LDAPException in case of a timeout.
    private static final String PATT_TIMEOUT_MESS2 = "SocketTimeoutException";
    													// another hint on a connection timeout.
    
    final static Boolean DEBUGLOG = false;        // log debug output.

    private static Logger jlog = LogManager.getLogger(LdapAuth3.class);

    public static String getErrMessage (int code) {
        switch (code) {
            case LDAP_AUTH_ROK:
                return "LDAP Authentication successful.";
            case LDAP_AUTH_RCONNECT:
                return "LDAP Authentication: connecting to LDAP Server failed!";
            case LDAP_AUTH_RINTERR:
                return "LDAP Authentication failed due to an internal error!";
            case LDAP_AUTH_RUNKNOWN:
                return "LDAP Authentication failed due to unknown user or password!";
            case LDAP_AUTH_RLOCKED:
                return "LDAP Authentication: known user is locked!";
            case LDAP_AUTH_RNOTREG:
                return "LDAP Authentication: known user, but not registered for this service!";
            case LDAP_AUTH_RNOEMAIL:
                return "LDAP Authentication: known user, but cannot obtain email!";
            case LDAP_AUTH_RNAUTH:
                return "LDAP Authentication: unknown user or password, or user is locked or not authorized!";
            default:
                return "LDAP Authentication failed with unknown error code!";
        }
    }

    /* LDAP Exception handling
     * 
     * isTimeout()
     * - somehow a dirty implementation, but documentation about timeouts is not very explicit.
     * - the INT value of TIMEOUT is currently 85.
     * - timeout return codes encountered with unboundid LDAP are 85 and 91.
     * Returns true in case of a timeout -> caller should return LDAP_AUTH_TIME.
     * 18.06.25/FB
     */
    
    private static boolean isTimeout(LDAPException e)
    
    {
    	if( e.getResultCode() == e.getResultCode().TIMEOUT )
	    	{
	    	return true; // LDAP_AUTH_TIMEOUT;	
	    	}
    	else if( e.getResultCode().intValue() == 91 || e.getResultCode().intValue() == 85 )
    		{
	    	if( e.getCause() != null )
	    		{
	    		String
	    			patterns = String.format("(%s|%s)",  PATT_TIMEOUT_MESS, PATT_TIMEOUT_MESS2);
	    		Pattern 
	    			pat = Pattern.compile(patterns, Pattern.CASE_INSENSITIVE);
	    		Matcher
	    			mat = pat.matcher(e.getCause().toString());
	    		
	    		boolean matched = mat.find();
	    		return matched;
	    		}
	    	}

   		return false;
    }
    
    // login
    
    public static int login (String login, String password,
            String ldapConfigFilename) throws LDAPException {
    	
        LDAPConfig ldapConfig = new LDAPConfig(ldapConfigFilename);

        login = Filter.encodeValue(login);
        password = Filter.encodeValue(password);

        if (ldapConfig.useEmbeddedServer) {
            try {
                EmbeddedLdapServer.startIfNotRunning(ldapConfig);
            }
            catch (GeneralSecurityException | UnknownHostException
                    | LDAPException e) {
                throw new RuntimeException(e);
            }
        }

        LdapAuth3Result ldapAuth3Result = search(login, password, ldapConfig,
                !ldapConfig.searchFilter.contains("${password}"), true);
        SearchResult srchRes = ldapAuth3Result.getSearchResultValue();

        if (ldapAuth3Result.getErrorCode() != 0 || srchRes == null
                || srchRes.getEntryCount() == 0) 
        {
            jlog.debug("Searching for '{}': ErrorCode={}, EntryCount={}, no entry found!\n", 
            		login, ldapAuth3Result.getErrorCode(), srchRes != null ? srchRes.getEntryCount() : 0);
            
            return ldapAuth3Result.getErrorCode();
        }

        return LDAP_AUTH_ROK;
    }

    @NotNull
    public static LdapAuth3Result search (String login, String password,
            LDAPConfig ldapConfig, boolean bindWithFoundDN,
            boolean applyExtraFilters) {
    	
        Map<String, String> valuesMap = new HashMap<>();
        valuesMap.put("login", login);
        valuesMap.put("password", password);
        StringSubstitutor sub = new StringSubstitutor(valuesMap);
        String searchFilterInstance = sub.replace(ldapConfig.searchFilter);

        valuesMap.clear();
        valuesMap.put("login", login);
        sub = new StringSubstitutor(valuesMap);
        String insensitiveSearchFilter = sub.replace(ldapConfig.searchFilter);

        LDAPConnection lc;

        if (ldapConfig.useSSL) {
            try {
                SSLUtil sslUtil;
                if (ldapConfig.trustStorePath != null
                        && !ldapConfig.trustStorePath.isEmpty()) {
                    sslUtil = new SSLUtil(new TrustStoreTrustManager(
                            ldapConfig.trustStorePath));
                }
                else {
                    sslUtil = new SSLUtil(new TrustAllTrustManager());
                }
                if (ldapConfig.additionalCipherSuites != null
                        && !ldapConfig.additionalCipherSuites.isEmpty()) {
                    addSSLCipherSuites(ldapConfig.additionalCipherSuites);
                }
                SSLSocketFactory socketFactory = sslUtil
                        .createSSLSocketFactory();
                lc = new LDAPConnection(socketFactory);
            }
            catch (GeneralSecurityException e) {
                //jlog.error(
            	jlog.error("login user '{}': Connecting to LDAPS Server: failed: {}!", login, e.toString());
                ldapTerminate(null);
                return new LdapAuth3Result(null, LDAP_AUTH_RCONNECT);
            }
        }
        else {
            lc = new LDAPConnection();
        }
        
        try {
        	// timeout - 18.06.25/FB
            lc.connect(ldapConfig.host, ldapConfig.port, ldapConfig.ldapTimeout);
            
            jlog.debug("{}: connect: successfull.", ldapConfig.useSSL ? "LDAPS" : "LDAP");
            }
        catch (LDAPException e) {
            jlog.error("Connecting to LDAP Server: failed: '{}'!\n", e);
            
            ldapTerminate(lc);
            return new LdapAuth3Result(null, isTimeout(e) ? LDAP_AUTH_RTIMEOUT : LDAP_AUTH_RCONNECT);
        }
        
        jlog.debug("isConnected={}.\n", lc.isConnected() ? "yes" : "no");

        try {
            // bind to server:
            if (DEBUGLOG)
                System.out.printf("Binding with '%s' ...\n",
                        ldapConfig.sLoginDN);
            lc.bind(ldapConfig.sLoginDN, ldapConfig.sPwd);
            
            if (DEBUGLOG)
                System.out.print("Binding: OK.\n");
        }
        catch (LDAPException e) {

        	jlog.error("login user '{}': binding failed: {}.", login, e.toString());
            ldapTerminate(lc);
            return new LdapAuth3Result(null, LDAP_AUTH_RINTERR);
        }

        jlog.debug("login: isConnected={}.", lc.isConnected() ? "yes" : "no");

        SearchResult srchRes = null;
        try {
        	jlog.debug("Searching with searchFilter: '{}'.", insensitiveSearchFilter);

            srchRes = lc.search(ldapConfig.searchBase, SearchScope.SUB,
                    searchFilterInstance);

            jlog.debug("Found '{}': {} entries.", login, srchRes.getEntryCount());
        }
        catch (LDAPSearchException e) {
        	
        	if( isTimeout(e) )
        		{
        		jlog.error("login user '{}': timeout reached: {}", login, e.toString());
	            ldapTerminate(lc);
	            return new LdapAuth3Result(null, LDAP_AUTH_RTIMEOUT);
        		}
        	else
        		jlog.error("login user '{}': no results!", login);
        }

        if (srchRes == null || srchRes.getEntryCount() == 0) {

            jlog.error("login user '{}': no entry found!", login);
            ldapTerminate(lc);
            return new LdapAuth3Result(null, LDAP_AUTH_RUNKNOWN);
        }

        if (bindWithFoundDN) {
            String matchedDN = srchRes.getSearchEntries().get(0).getDN();
            if (DEBUGLOG)
                System.out.printf("Requested bind for found user %s' failed.\n",
                        matchedDN);
            try {
                // bind to server:
                if (DEBUGLOG)
                    System.out.printf("Binding with '%s' ...\n", matchedDN);
                BindResult bindResult = lc.bind(matchedDN, password);
                if (DEBUGLOG)
                    System.out.print("Binding: OK.\n");
                if (!bindResult.getResultCode().equals(ResultCode.SUCCESS)) {
                    ldapTerminate(lc);
                    return new LdapAuth3Result(null, LDAP_AUTH_RUNKNOWN);
                }
            }
            catch (LDAPException e) {

                jlog.error("login user '{}': binding with DN failed: {}. ", login, e.toString());
                ldapTerminate(lc);
                return new LdapAuth3Result(null, isTimeout(e) ? LDAP_AUTH_RTIMEOUT : LDAP_AUTH_RUNKNOWN);
            }
        }

        if (applyExtraFilters) {
            if (ldapConfig.authFilter != null
                    && !ldapConfig.authFilter.isEmpty()) {
                srchRes = applyAdditionalFilter(login, ldapConfig,
                        ldapConfig.authFilter, searchFilterInstance, lc);
                if (srchRes == null || srchRes.getEntryCount() == 0) {
                    ldapTerminate(lc);
                    return new LdapAuth3Result(null, LDAP_AUTH_RNOTREG);
                }
            }

            if (ldapConfig.userNotBlockedFilter != null
                    && !ldapConfig.userNotBlockedFilter.isEmpty()) {
                srchRes = applyAdditionalFilter(login, ldapConfig,
                        ldapConfig.userNotBlockedFilter, searchFilterInstance,
                        lc);
                if (srchRes == null || srchRes.getEntryCount() == 0) {
                    ldapTerminate(lc);
                    return new LdapAuth3Result(null, LDAP_AUTH_RLOCKED);
                }
            }
        }

        ldapTerminate(lc);
        return new LdapAuth3Result(srchRes, LDAP_AUTH_ROK);
    }

    private static SearchResult applyAdditionalFilter (String login,
            LDAPConfig ldapConfig, String searchFilterInstance,
            String extraFilter, LDAPConnection lc) {
        SearchResult srchRes;
        srchRes = null;
        try {
            String combindedFilterInstance = "(&" + searchFilterInstance
                    + extraFilter + ")";
            if (DEBUGLOG)
                System.out.printf("Searching with additional Filter: '%s'.\n",
                        extraFilter);
            srchRes = lc.search(ldapConfig.searchBase, SearchScope.SUB,
                    combindedFilterInstance);
            if (DEBUGLOG)
                System.out.printf("Found '%s': %d entries.\n", login,
                        srchRes.getEntryCount());
        }
        catch (LDAPSearchException e) {
            jlog.error("Error: Search for User failed: '%s'!\n", e);
        }
        return srchRes;
    }

    public static String getEmail (String sUserDN, String ldapConfigFilename)
            throws LDAPException {
        String sUserPwd = "*";
        LDAPConfig ldapConfig = new LDAPConfig(ldapConfigFilename);
        final String emailAttribute = ldapConfig.emailAttribute;

        SearchResult searchResult = search(sUserDN, sUserPwd, ldapConfig, false,
                false).getSearchResultValue();

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

    public static String getUsername (String sUserDN, String ldapConfigFilename)
            throws LDAPException {
        String sUserPwd = "*";
        LDAPConfig ldapConfig = new LDAPConfig(ldapConfigFilename);
        final String idsC2Attribute = "idsC2Profile";
        final String uidAttribute = "uid";

        SearchResult searchResult = search(sUserDN, sUserPwd, ldapConfig, false,
                false).getSearchResultValue();

        if (searchResult == null) {
            return null;
        }

        String username = null;
        for (SearchResultEntry entry : searchResult.getSearchEntries()) {
            username = entry.getAttributeValue(idsC2Attribute);
            if (username == null) {
                username = entry.getAttributeValue(uidAttribute);
                jlog.warn("idsC2Profile not found for uid: " + username);
            }
        }
        return username;
    }

    public static void ldapTerminate (LDAPConnection lc) {
        if (DEBUGLOG)
            System.out.println("Terminating...");

        if (lc != null) {
            lc.close(null);
        }
        if (DEBUGLOG)
            System.out.println("closing connection: done.\n");
    }

    private static void addSSLCipherSuites (String ciphersCsv) {
        // add e.g. TLS_RSA_WITH_AES_256_GCM_SHA384
        Set<String> ciphers = new HashSet<>();
        ciphers.addAll(SSLUtil.getEnabledSSLCipherSuites());
        ciphers.addAll(Arrays.asList(ciphersCsv.split(", *")));
        SSLUtil.setEnabledSSLCipherSuites(ciphers);
    }

    public static class LdapAuth3Result {
        final int errorCode;
        final Object value;

        public LdapAuth3Result (Object value, int errorCode) {
            this.errorCode = errorCode;
            this.value = value;
        }

        public int getErrorCode () {
            return errorCode;
        }

        public Object getValue () {
            return value;
        }

        public SearchResult getSearchResultValue () {
            return (SearchResult) value;
        }
    }
}

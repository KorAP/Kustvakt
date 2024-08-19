package de.ids_mannheim.korap.config;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.ids_mannheim.korap.constant.AuthenticationMethod;
import de.ids_mannheim.korap.interfaces.EncryptionIface;
import de.ids_mannheim.korap.utils.TimeUtils;

/**
 * Configuration for Kustvakt full version including properties
 * concerning authentication and licenses.
 * 
 * @author margaretha
 *
 */

public class FullConfiguration extends KustvaktConfiguration {
    public static Logger jlog = LogManager.getLogger(FullConfiguration.class);

    private String ldapConfig;

    private String freeOnlyRegex;
    private String publicOnlyRegex;
    private String allOnlyRegex;

    private List<String> freeRegexList;
    private List<String> publicRegexList;
    private List<String> allRegexList;

    private String authenticationScheme;

    private EncryptionIface.Encryption secureHashAlgorithm;

    private AuthenticationMethod OAuth2passwordAuthentication;
    private String nativeClientHost;
    private Set<String> clientCredentialsScopes;
    private int maxAuthenticationAttempts;

    private int accessTokenLongExpiry;
    private int accessTokenExpiry;
    private int refreshTokenLongExpiry;
    private int refreshTokenExpiry;
    private int authorizationCodeExpiry;

    private int maxNumberOfUserQueries;

    private URL issuer;

    private String namedVCPath;

    private boolean createInitialSuperClient;

    public FullConfiguration (Properties properties) throws Exception {
        super(properties);
    }

    public FullConfiguration () {
        super();
    }

    @Override
    public void load (Properties properties) throws Exception {

        super.load(properties);
        // EM: regex used for storing vc
        setLicenseRegex(properties);

        // EM: pattern for matching availability in Krill matches
        setLicensePatterns(properties);
        ldapConfig = properties.getProperty("ldap.config");

        setSecurityConfiguration(properties);
        setOAuth2Configuration(properties);

        setNamedVCPath(properties.getProperty("krill.namedVC", ""));

        //        Cache cache = CacheManager.newInstance().getCache("named_vc");
        //        CacheConfiguration config = cache.getCacheConfiguration();
        //        config.setMaxBytesLocalHeap(properties.getProperty("cache.max.bytes.local.heap", "256m"));
        //        config.setMaxBytesLocalDisk(properties.getProperty("cache.max.bytes.local.disk", "2G"));
        //        jlog.info("max local heap:"+config.getMaxBytesLocalHeapAsString());
        //        jlog.info("max local disk:"+config.getMaxBytesLocalDiskAsString());

        setMaxNumberOfUserQueries(Integer.parseInt(
                properties.getProperty("max.user.persistent.queries", "20")));
    }

    private void setSecurityConfiguration (Properties properties)
            throws MalformedURLException {
        setSecureHashAlgorithm(Enum.valueOf(EncryptionIface.Encryption.class,
                properties.getProperty("security.secure.hash.algorithm",
                        "BCRYPT")));

        String issuerStr = properties.getProperty("security.jwt.issuer",
                "https://korap.ids-mannheim.de");

        if (!issuerStr.startsWith("http")) {
            issuerStr = "http://" + issuerStr;
        }
        setIssuer(new URL(issuerStr));
    }

    private void setOAuth2Configuration (Properties properties) {
        setOAuth2passwordAuthentication(
                Enum.valueOf(AuthenticationMethod.class, properties.getProperty(
                        "oauth2.password.authentication", "TEST")));
        setNativeClientHost(properties.getProperty("oauth2.native.client.host",
                "korap.ids-mannheim.de"));
        setCreateInitialSuperClient(Boolean.valueOf(properties
                .getProperty("oauth2.initial.super.client", "false")));

        setMaxAuthenticationAttempts(Integer
                .parseInt(properties.getProperty("oauth2.max.attempts", "1")));

        String clientScopes = properties
                .getProperty("oauth2.client.credentials.scopes", "client_info");
        setClientCredentialsScopes(Arrays.stream(clientScopes.split(" "))
                .collect(Collectors.toSet()));

        accessTokenExpiry = TimeUtils.convertTimeToSeconds(
                properties.getProperty("oauth2.access.token.expiry", "1D"));
        refreshTokenExpiry = TimeUtils.convertTimeToSeconds(
                properties.getProperty("oauth2.refresh.token.expiry", "90D"));
        authorizationCodeExpiry = TimeUtils.convertTimeToSeconds(properties
                .getProperty("oauth2.authorization.code.expiry", "10M"));

        setAccessTokenLongExpiry(TimeUtils.convertTimeToSeconds(properties
                .getProperty("oauth2.access.token.long.expiry", "365D")));
        setRefreshTokenLongExpiry(TimeUtils.convertTimeToSeconds(properties
                .getProperty("oauth2.refresh.token.long.expiry", "365D")));
    }

    private void setLicensePatterns (Properties properties) {
        setFreeLicensePattern(compilePattern(getFreeOnlyRegex()));
        setPublicLicensePattern(compilePattern(
                getFreeOnlyRegex() + "|" + getPublicOnlyRegex()));
        setAllLicensePattern(compilePattern(getFreeOnlyRegex() + "|"
                + getPublicOnlyRegex() + "|" + getAllOnlyRegex()));
    }

    private void setLicenseRegex (Properties properties) {
        setFreeOnlyRegex(properties.getProperty("availability.regex.free", ""));
        freeRegexList = splitAndAddToList(getFreeOnlyRegex());

        setPublicOnlyRegex(
                properties.getProperty("availability.regex.public", ""));
        publicRegexList = splitAndAddToList(getPublicOnlyRegex());

        setAllOnlyRegex(properties.getProperty("availability.regex.all", ""));
        allRegexList = splitAndAddToList(getAllOnlyRegex());
    }

    private List<String> splitAndAddToList (String regex) {
        List<String> list;
        if (regex.contains("|")) {
            String[] regexes = regex.split("\\|");
            list = new ArrayList<>(regexes.length);
            for (String s : regexes) {
                list.add(s.trim());
            }
        }
        else {
            list = new ArrayList<>(1);
            list.add(regex);
        }
        return list;
    }

    private Pattern compilePattern (String patternStr) {
        if (!patternStr.isEmpty()) {
            return Pattern.compile(patternStr);
        }
        else {
            return null;
        }
    }

    public String getLdapConfig () {
        return ldapConfig;
    }

    public Pattern getPublicLicensePattern () {
        return publicLicensePattern;
    }

    public void setPublicLicensePattern (Pattern publicLicensePattern) {
        this.publicLicensePattern = publicLicensePattern;
    }

    public Pattern getFreeLicensePattern () {
        return freeLicensePattern;
    }

    public void setFreeLicensePattern (Pattern freeLicensePattern) {
        this.freeLicensePattern = freeLicensePattern;
    }

    public Pattern getAllLicensePattern () {
        return allLicensePattern;
    }

    public void setAllLicensePattern (Pattern allLicensePattern) {
        this.allLicensePattern = allLicensePattern;
    }

    public String getAuthenticationScheme () {
        return authenticationScheme;
    }

    public void setAuthenticationScheme (String authenticationScheme) {
        this.authenticationScheme = authenticationScheme;
    }

    public List<String> getFreeRegexList () {
        return freeRegexList;
    }

    public void setFreeRegexList (List<String> freeRegexList) {
        this.freeRegexList = freeRegexList;
    }

    public List<String> getPublicRegexList () {
        return publicRegexList;
    }

    public void setPublicRegexList (List<String> publicRegexList) {
        this.publicRegexList = publicRegexList;
    }

    public List<String> getAllRegexList () {
        return allRegexList;
    }

    public void setAllRegexList (List<String> allRegexList) {
        this.allRegexList = allRegexList;
    }

    public String getFreeOnlyRegex () {
        return freeOnlyRegex;
    }

    public void setFreeOnlyRegex (String freeOnlyRegex) {
        this.freeOnlyRegex = freeOnlyRegex;
    }

    public String getPublicOnlyRegex () {
        return publicOnlyRegex;
    }

    public void setPublicOnlyRegex (String publicOnlyRegex) {
        this.publicOnlyRegex = publicOnlyRegex;
    }

    public String getAllOnlyRegex () {
        return allOnlyRegex;
    }

    public void setAllOnlyRegex (String allOnlyRegex) {
        this.allOnlyRegex = allOnlyRegex;
    }

    public EncryptionIface.Encryption getSecureHashAlgorithm () {
        return secureHashAlgorithm;
    }

    public void setSecureHashAlgorithm (
            EncryptionIface.Encryption secureHashAlgorithm) {
        this.secureHashAlgorithm = secureHashAlgorithm;
    }

    public AuthenticationMethod getOAuth2passwordAuthentication () {
        return OAuth2passwordAuthentication;
    }

    public void setOAuth2passwordAuthentication (
            AuthenticationMethod oAuth2passwordAuthentication) {
        OAuth2passwordAuthentication = oAuth2passwordAuthentication;
    }

    public String getNativeClientHost () {
        return nativeClientHost;
    }

    public void setNativeClientHost (String nativeClientHost) {
        this.nativeClientHost = nativeClientHost;
    }

    public int getMaxAuthenticationAttempts () {
        return maxAuthenticationAttempts;
    }

    public void setMaxAuthenticationAttempts (int maxAuthenticationAttempts) {
        this.maxAuthenticationAttempts = maxAuthenticationAttempts;
    }

    public Set<String> getClientCredentialsScopes () {
        return clientCredentialsScopes;
    }

    public void setClientCredentialsScopes (
            Set<String> clientCredentialsScopes) {
        this.clientCredentialsScopes = clientCredentialsScopes;
    }

    public URL getIssuer () {
        return issuer;
    }

    public void setIssuer (URL issuer) {
        this.issuer = issuer;
    }

    public int getAccessTokenExpiry () {
        return accessTokenExpiry;
    }

    public void setAccessTokenExpiry (int accessTokenExpiry) {
        this.accessTokenExpiry = accessTokenExpiry;
    }

    public int getRefreshTokenExpiry () {
        return refreshTokenExpiry;
    }

    public void setRefreshTokenExpiry (int refreshTokenExpiry) {
        this.refreshTokenExpiry = refreshTokenExpiry;
    }

    public int getAuthorizationCodeExpiry () {
        return authorizationCodeExpiry;
    }

    public void setAuthorizationCodeExpiry (int authorizationCodeExpiry) {
        this.authorizationCodeExpiry = authorizationCodeExpiry;
    }

    public String getNamedVCPath () {
        return namedVCPath;
    }

    public void setNamedVCPath (String namedVCPath) {
        this.namedVCPath = namedVCPath;
    }

    public int getAccessTokenLongExpiry () {
        return accessTokenLongExpiry;
    }

    public void setAccessTokenLongExpiry (int accessTokenLongExpiry) {
        this.accessTokenLongExpiry = accessTokenLongExpiry;
    }

    public int getRefreshTokenLongExpiry () {
        return refreshTokenLongExpiry;
    }

    public void setRefreshTokenLongExpiry (int refreshTokenLongExpiry) {
        this.refreshTokenLongExpiry = refreshTokenLongExpiry;
    }

    public boolean createInitialSuperClient () {
        return createInitialSuperClient;
    }

    public void setCreateInitialSuperClient (boolean initialSuperClient) {
        this.createInitialSuperClient = initialSuperClient;
    }

    public int getMaxNumberOfUserQueries () {
        return maxNumberOfUserQueries;
    }

    public void setMaxNumberOfUserQueries (int maxNumberOfUserQueries) {
        this.maxNumberOfUserQueries = maxNumberOfUserQueries;
    }

}

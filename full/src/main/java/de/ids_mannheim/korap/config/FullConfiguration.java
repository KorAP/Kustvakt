package de.ids_mannheim.korap.config;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.security.interfaces.RSAPrivateKey;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.util.IOUtils;

import de.ids_mannheim.korap.constant.AuthenticationMethod;
import de.ids_mannheim.korap.interfaces.EncryptionIface;
import de.ids_mannheim.korap.oauth2.openid.OpenIdConfiguration;
import de.ids_mannheim.korap.utils.TimeUtils;

/**
 * Configuration for Kustvakt full version including properties
 * concerning authentication and licenses.
 * 
 * @author margaretha
 *
 */

public class FullConfiguration extends KustvaktConfiguration {
    // mail configuration
    private boolean isMailEnabled;
    private String testEmail;
    private String noReply;
    private String emailAddressRetrieval;

    private String groupInvitationTemplate;

    private String ldapConfig;

    private String freeOnlyRegex;
    private String publicOnlyRegex;
    private String allOnlyRegex;

    private List<String> freeRegexList;
    private List<String> publicRegexList;
    private List<String> allRegexList;

    private Pattern publicLicensePattern;
    private Pattern freeLicensePattern;
    private Pattern allLicensePattern;

    private String authenticationScheme;

    private boolean isSoftDeleteAutoGroup;
    private boolean isSoftDeleteGroup;
    private boolean isSoftDeleteGroupMember;

    private EncryptionIface.Encryption secureHashAlgorithm;
    private String secureRandomAlgorithm;
    private String messageDigestAlgorithm;

    private AuthenticationMethod OAuth2passwordAuthentication;
    private String nativeClientHost;
    private Set<String> defaultAccessScopes;
    private Set<String> clientCredentialsScopes;
    private int maxAuthenticationAttempts;

    private int accessTokenExpiry;
    private int refreshTokenExpiry;
    private int authorizationCodeExpiry;

    private URL issuer;
    private URI issuerURI;
    private OpenIdConfiguration openidConfig;

    private RSAPrivateKey rsaPrivateKey;
    private JWKSet publicKeySet;
    private String rsaKeyId;
    
    private String namedVCPath;

    public FullConfiguration (Properties properties) throws Exception {
        super(properties);
    }

    @Override
    public void load (Properties properties) throws Exception {

        super.load(properties);
        // EM: regex used for storing vc
        setLicenseRegex(properties);

        // EM: pattern for matching availability in Krill matches
        setLicensePatterns(properties);
        setDeleteConfiguration(properties);
        setMailConfiguration(properties);
        ldapConfig = properties.getProperty("ldap.config");

        setSecurityConfiguration(properties);
        setOAuth2Configuration(properties);
        setOpenIdConfiguration(properties);
        setRSAKeys(properties);
        
        setNamedVCPath(properties
                .getProperty("krill.namedVC", ""));
    }

    private void setSecurityConfiguration (Properties properties) {
        setSecureHashAlgorithm(Enum.valueOf(EncryptionIface.Encryption.class,
                properties.getProperty("security.secure.hash.algorithm",
                        "BCRYPT")));

        setSecureRandomAlgorithm(properties
                .getProperty("security.secure.random.algorithm", "SHA1PRNG"));

        setMessageDigestAlgorithm(
                properties.getProperty("security.md.algorithm", "MD5"));
    }

    private void setOpenIdConfiguration (Properties properties)
            throws URISyntaxException, MalformedURLException {
        String issuerStr = properties.getProperty("security.jwt.issuer",
                "https://korap.ids-mannheim.de");

        if (!issuerStr.startsWith("http")) {
            issuerStr = "http://" + issuerStr;
        }
        setIssuer(new URL(issuerStr));
        setIssuerURI(issuer.toURI());

        issuerStr = issuerURI.toString();

        OpenIdConfiguration openidConfig = new OpenIdConfiguration();
        openidConfig.setIssuer(issuerStr);
        openidConfig.setJwks_uri(issuerStr + OpenIdConfiguration.JWKS_ENDPOINT);
        openidConfig.setRegistration_endpoint(
                issuerStr + OpenIdConfiguration.CLIENT_REGISTRATION_ENDPOINT);
        openidConfig.setAuthorization_endpoint(
                issuerStr + OpenIdConfiguration.AUTHORIZATION_ENDPOINT);
        openidConfig.setToken_endpoint(
                issuerStr + OpenIdConfiguration.TOKEN_ENDPOINT);

        String grantTypes = properties.getProperty("openid.grant.types", "");
        openidConfig.setGrant_types_supported(grantTypes.split(" "));

        String responseTypes =
                properties.getProperty("openid.response.types", "code");
        openidConfig.setResponse_types_supported(responseTypes.split(" "));

        String responseModes =
                properties.getProperty("openid.response.modes", "");
        openidConfig.setResponse_modes_supported(responseModes.split(" "));

        String clientAuthMethods =
                properties.getProperty("openid.client.auth.methods", "");
        openidConfig.setToken_endpoint_auth_methods_supported(
                clientAuthMethods.split(" "));

        String tokenSigningAlgorithms = properties
                .getProperty("openid.token.signing.algorithms", "RS256");
        openidConfig.setToken_endpoint_auth_signing_alg_values_supported(
                tokenSigningAlgorithms.split(" "));

        String subjectTypes =
                properties.getProperty("openid.subject.types", "public");
        openidConfig.setSubject_types_supported(subjectTypes.split(" "));

        String displayTypes =
                properties.getProperty("openid.display.types", "");
        openidConfig.setDisplay_values_supported(displayTypes.split(" "));

        String supportedScopes =
                properties.getProperty("openid.supported.scopes", "");
        openidConfig.setScopes_supported(supportedScopes.split(" "));

        String claimTypes =
                properties.getProperty("openid.claim.types", "normal");
        openidConfig.setClaim_types_supported(claimTypes.split(" "));

        String supportedClaims =
                properties.getProperty("openid.supported.claims", "");
        openidConfig.setClaims_supported(supportedClaims.split(" "));

        String claimLocales =
                properties.getProperty("openid.supported.claim.locales", "");
        openidConfig.setClaims_locales_supported(claimLocales.split(" "));

        String uiLocales = properties.getProperty("openid.ui.locales", "en");
        openidConfig.setUi_locales_supported(uiLocales.split(" "));

        boolean supportClaimParam = Boolean.getBoolean(
                properties.getProperty("openid.support.claim.param", "false"));
        openidConfig.setClaims_parameter_supported(supportClaimParam);

        openidConfig.setRequest_parameter_supported(false);
        openidConfig.setRequest_uri_parameter_supported(false);
        openidConfig.setRequire_request_uri_registration(false);
        openidConfig.setMutual_tls_sender_constrained_access_tokens(false);

        String privacyPolicy =
                properties.getProperty("openid.privacy.policy", "");
        openidConfig.setOp_policy_uri(privacyPolicy);

        String termOfService =
                properties.getProperty("openid.term.of.service", "");
        openidConfig.setOp_tos_uri(termOfService);

        String serviceDocURL = properties.getProperty("openid.service.doc", "");
        openidConfig.setService_documentation(serviceDocURL);
        this.setOpenidConfig(openidConfig);
    }

    private void setRSAKeys (Properties properties)
            throws IOException, ParseException, JOSEException {
        setRsaKeyId(properties.getProperty("rsa.key.id", ""));

        String rsaPublic = properties.getProperty("rsa.public", null);
        setPublicKeySet(rsaPublic);

        String rsaPrivate = properties.getProperty("rsa.private", null);
        setRsaPrivateKey(rsaPrivate);
    }

    private void setOAuth2Configuration (Properties properties) {
        setOAuth2passwordAuthentication(
                Enum.valueOf(AuthenticationMethod.class, properties.getProperty(
                        "oauth2.password.authentication", "TEST")));
        setNativeClientHost(properties.getProperty("oauth2.native.client.host",
                "korap.ids-mannheim.de"));

        setMaxAuthenticationAttempts(Integer
                .parseInt(properties.getProperty("oauth2.max.attempts", "1")));

        String scopes = properties.getProperty("oauth2.default.scopes",
                "openid preferred_username");
        Set<String> scopeSet =
                Arrays.stream(scopes.split(" ")).collect(Collectors.toSet());
        setDefaultAccessScopes(scopeSet);

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
    }

    private void setMailConfiguration (Properties properties) {
        setMailEnabled(Boolean
                .valueOf(properties.getProperty("mail.enabled", "false")));
        if (isMailEnabled) {
            // other properties must be set in the kustvakt.conf
            setTestEmail(
                    properties.getProperty("mail.receiver", "test@localhost"));
            setNoReply(properties.getProperty("mail.sender"));
            setGroupInvitationTemplate(
                    properties.getProperty("template.group.invitation"));
            setEmailAddressRetrieval(
                    properties.getProperty("mail.address.retrieval", "test"));
        }
    }

    private void setDeleteConfiguration (Properties properties) {
        setSoftDeleteGroup(
                parseDeleteConfig(properties.getProperty("delete.group", "")));
        setSoftDeleteAutoGroup(parseDeleteConfig(
                properties.getProperty("delete.auto.group", "")));
        setSoftDeleteGroupMember(parseDeleteConfig(
                properties.getProperty("delete.group.member", "")));
    }

    private boolean parseDeleteConfig (String deleteConfig) {
        return deleteConfig.equals("soft") ? true : false;
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

    public boolean isSoftDeleteGroup () {
        return isSoftDeleteGroup;
    }

    public void setSoftDeleteGroup (boolean isSoftDeleteGroup) {
        this.isSoftDeleteGroup = isSoftDeleteGroup;
    }

    public boolean isSoftDeleteGroupMember () {
        return isSoftDeleteGroupMember;
    }

    public void setSoftDeleteGroupMember (boolean isSoftDeleteGroupMember) {
        this.isSoftDeleteGroupMember = isSoftDeleteGroupMember;
    }

    public boolean isSoftDeleteAutoGroup () {
        return isSoftDeleteAutoGroup;
    }

    public void setSoftDeleteAutoGroup (boolean isSoftDeleteAutoGroup) {
        this.isSoftDeleteAutoGroup = isSoftDeleteAutoGroup;
    }

    public String getTestEmail () {
        return testEmail;
    }

    public void setTestEmail (String testEmail) {
        this.testEmail = testEmail;
    }

    public boolean isMailEnabled () {
        return isMailEnabled;
    }

    public void setMailEnabled (boolean isMailEnabled) {
        this.isMailEnabled = isMailEnabled;
    }

    public String getNoReply () {
        return noReply;
    }

    public void setNoReply (String noReply) {
        this.noReply = noReply;
    }

    public String getGroupInvitationTemplate () {
        return groupInvitationTemplate;
    }

    public void setGroupInvitationTemplate (String groupInvitationTemplate) {
        this.groupInvitationTemplate = groupInvitationTemplate;
    }

    public String getEmailAddressRetrieval () {
        return emailAddressRetrieval;
    }

    public void setEmailAddressRetrieval (String emailAddressRetrieval) {
        this.emailAddressRetrieval = emailAddressRetrieval;
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

    public Set<String> getDefaultAccessScopes () {
        return defaultAccessScopes;
    }

    public void setDefaultAccessScopes (Set<String> accessScopes) {
        this.defaultAccessScopes = accessScopes;
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

    public URI getIssuerURI () {
        return issuerURI;
    }

    public void setIssuerURI (URI issuerURI) {
        this.issuerURI = issuerURI;
    }

    public JWKSet getPublicKeySet () {
        return publicKeySet;
    }

    public void setPublicKeySet (String rsaPublic)
            throws IOException, ParseException {
        if (rsaPublic == null || rsaPublic.isEmpty()) {
            return;
        }

        File rsaPublicFile = new File(rsaPublic);
        JWKSet jwkSet = null;
        InputStream is = null;
        if (rsaPublicFile.exists()) {
            jwkSet = JWKSet.load(rsaPublicFile);
        }
        else if ((is = getClass().getClassLoader()
                .getResourceAsStream(rsaPublic)) != null) {
            jwkSet = JWKSet.load(is);
        }
        this.publicKeySet = jwkSet;
    }

    public RSAPrivateKey getRsaPrivateKey () {
        return rsaPrivateKey;
    }

    public void setRsaPrivateKey (String rsaPrivate)
            throws IOException, ParseException, JOSEException {
        if (rsaPrivate == null || rsaPrivate.isEmpty()) {
            return;
        }
        File rsaPrivateFile = new File(rsaPrivate);
        String keyString = null;
        InputStream is = null;
        if (rsaPrivateFile.exists()) {
            keyString = IOUtils.readFileToString(rsaPrivateFile,
                    Charset.forName("UTF-8"));
        }
        else if ((is = getClass().getClassLoader()
                .getResourceAsStream(rsaPrivate)) != null) {
            keyString = IOUtils.readInputStreamToString(is,
                    Charset.forName("UTF-8"));
        }
        RSAKey rsaKey = (RSAKey) JWK.parse(keyString);
        this.rsaPrivateKey = (RSAPrivateKey) rsaKey.toPrivateKey();
    }

    public String getRsaKeyId () {
        return rsaKeyId;
    }

    public void setRsaKeyId (String rsaKeyId) {
        this.rsaKeyId = rsaKeyId;
    }

    public OpenIdConfiguration getOpenidConfig () {
        return openidConfig;
    }

    public void setOpenidConfig (OpenIdConfiguration openidConfig) {
        this.openidConfig = openidConfig;
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

    public String getSecureRandomAlgorithm () {
        return secureRandomAlgorithm;
    }

    public void setSecureRandomAlgorithm (String secureRandomAlgorithm) {
        this.secureRandomAlgorithm = secureRandomAlgorithm;
    }

    public String getMessageDigestAlgorithm () {
        return messageDigestAlgorithm;
    }

    public void setMessageDigestAlgorithm (String messageDigestAlgorithm) {
        this.messageDigestAlgorithm = messageDigestAlgorithm;
    }

    public String getNamedVCPath () {
        return namedVCPath;
    }

    public void setNamedVCPath (String namedVCPath) {
        this.namedVCPath = namedVCPath;
    }
}

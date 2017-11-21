package de.ids_mannheim.korap.config;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Pattern;

import org.apache.log4j.PropertyConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.interfaces.EncryptionIface;
import de.ids_mannheim.korap.utils.TimeUtils;
import lombok.Getter;

/**
 * if configuration class is extended, loadSubTypes method should be
 * overriden
 * 
 * @author hanl, margaretha
 * @date 05/02/2014
 */

@Getter
public class KustvaktConfiguration {

    public static final Map<String, Object> KUSTVAKT_USER = new HashMap<>();

    private static final Logger jlog = LoggerFactory
            .getLogger(KustvaktConfiguration.class);
    private String indexDir;
    private int port;
    // todo: make exclusive so that the containg languages can really only be used then
    private List<String> queryLanguages;

    private String serverHost;
    private URL issuer;

    private int maxhits;
    private int returnhits;
    private String keystoreLocation;
    private String keystorePassword;
    private Properties mailProperties;
    private String host;
    private String shibUserMapping;
    private String userConfig;
    private int inactiveTime;
    private int loginAttemptTTL;
    private long loginAttemptNum;
    private boolean allowMultiLogIn;
    private int loadFactor;
    @Deprecated
    private int validationStringLength;
    @Deprecated
    private int validationEmaillength;
    // fixme: should move to base config?!
    private EncryptionIface.Encryption encryption;
    private byte[] sharedSecret;
    @Deprecated
    private String adminToken;
    private int longTokenTTL;
    private int tokenTTL;
    private int shortTokenTTL;
    private String[] rewrite_strategies;
    private String passcodeSaltField;

    private String default_pos;
    private String default_lemma;
    private String default_token;
    private String default_dep;
    private String default_const;
    @Deprecated    
    private String policyConfig;
    private ArrayList<String> foundries;
    private ArrayList<String> layers;
    
    private Pattern publicLicensePattern;
    private Pattern freeLicensePattern;
    private Pattern allLicensePattern;
    
    private String baseURL;
    
    
    // deprec?!
    private final BACKENDS DEFAULT_ENGINE = BACKENDS.LUCENE;

	private String ldapConfig;

    private String freeOnlyRegex;

    private String publicOnlyRegex;

    private String allOnlyRegex;

	public KustvaktConfiguration (Properties properties) throws IOException {
        load(properties);
    }
	
    /**
     * loading of the properties and mapping to parameter variables
     * 
     * @param properties
     * @return
     * @throws IOException 
     * @throws KustvaktException 
     */
    protected Properties load (Properties properties)
            throws IOException {
        baseURL = properties.getProperty("kustvakt.base.url", "/api/*");
        maxhits = new Integer(properties.getProperty("maxhits", "50000"));
        returnhits = new Integer(properties.getProperty("returnhits", "50000"));
        indexDir = properties.getProperty("krill.indexDir", "");
        port = new Integer(properties.getProperty("server.port", "8095"));
        // server options
        serverHost = String.valueOf(properties.getProperty("server.host",
                "localhost"));
        String queries = properties.getProperty("korap.ql", "");
        String[] qls = queries.split(",");
        queryLanguages = new ArrayList<>();
        for (String querylang : qls)
            queryLanguages.add(querylang.trim().toUpperCase());
        String is = properties.getProperty("kustvakt.security.jwt.issuer", "");

        if (!is.startsWith("http"))
            is = "http://" + is;
        issuer = new URL(is);

        default_const = properties
                .getProperty("default.layer.c", "mate");
        default_dep = properties.getProperty("default.layer.d", "mate");
        default_lemma = properties.getProperty("default.layer.l", "tt");
        default_pos = properties.getProperty("default.layer.p", "tt");
        default_token = properties.getProperty("default.layer.o",
                "opennlp");

        // security configuration
        inactiveTime = TimeUtils.convertTimeToSeconds(properties.getProperty(
                "security.idleTimeoutDuration", "10M"));
        allowMultiLogIn = Boolean.valueOf(properties
                .getProperty("security.multipleLogIn"));

        loginAttemptNum = Long.parseLong(properties.getProperty(
                "security.loginAttemptNum", "3"));
        loginAttemptTTL = TimeUtils.convertTimeToSeconds(properties
                .getProperty("security.authAttemptTTL", "30M"));

        loadFactor = Integer.valueOf(properties.getProperty(
                "security.encryption.loadFactor", "15"));
        validationStringLength = Integer.valueOf(properties.getProperty(
                "security.validation.stringLength", "150"));
        validationEmaillength = Integer.valueOf(properties.getProperty(
                "security.validation.emailLength", "40"));
        encryption = Enum.valueOf(EncryptionIface.Encryption.class,
                properties.getProperty("security.encryption", "BCRYPT"));
        sharedSecret = properties.getProperty("security.sharedSecret", "")
                .getBytes();
        adminToken = properties.getProperty("security.adminToken");

        longTokenTTL = TimeUtils.convertTimeToSeconds(properties.getProperty(
                "security.longTokenTTL", "100D"));
        tokenTTL = TimeUtils.convertTimeToSeconds(properties.getProperty(
                "security.tokenTTL", "72H"));
        shortTokenTTL = TimeUtils.convertTimeToSeconds(properties.getProperty(
                "security.shortTokenTTL", "3H"));

        passcodeSaltField = properties.getProperty("security.passcode.salt",
                "accountCreation");
        
        ldapConfig = properties.getProperty("ldap.config");
        
        // EM: regex used for storing vc
        freeOnlyRegex = properties.getProperty("availability.regex.free","");
        publicOnlyRegex = properties.getProperty("availability.regex.public","");
        allOnlyRegex = properties.getProperty("availability.regex.all","");
        
        // EM: pattern for matching availability in Krill matches
        freeLicensePattern = compilePattern(freeOnlyRegex);
        publicLicensePattern = compilePattern(freeOnlyRegex + "|" + publicOnlyRegex);
        allLicensePattern = compilePattern(freeOnlyRegex + "|" + publicOnlyRegex + "|"+allOnlyRegex);
        
        return properties;
    }

    private Pattern compilePattern (String patternStr) {
        if (!patternStr.isEmpty()){
            return Pattern.compile(patternStr);    
        }
        else{
            return null;
        }
    }

    public void setFoundriesAndLayers(String config) throws IOException {
    	foundries = new ArrayList<String>();
    	layers = new ArrayList<String>();
    	
    	BufferedReader br;
		File f = new File(config);
		br = new BufferedReader(new InputStreamReader(new FileInputStream(f)));
		String policy = null;
		String[] policyData = null;
		String type, layer;
			while ((policy = br.readLine()) != null) {
				if (policy.startsWith("#") || policy.isEmpty()){
					continue;
				}
				policyData = policy.split("\t");
				type = policyData[0];
				if (type.equals("foundry")){
					foundries.add(policyData[1]);
				}
				else if (type.equals("layer")){
					layer = policyData[1].split("/")[1];
					layers.add(layer);
				}
			}
	}
    
    
    /**
     * set properties
     * 
     * @param props
     * @throws IOException 
     */
//    public void setProperties (Properties props) throws IOException {
//        this.load(props);
//    }


    /**
     * properties can be overloaded after spring init
     * 
     * @param stream
     */
    public void setPropertiesAsStream (InputStream stream) {
        try {

            Properties p = new Properties();
            p.load(stream);
            this.load(p);
        }
        catch (IOException e) {
            e.printStackTrace();
        }

    }


    public BACKENDS chooseBackend (String value) {
        if (value == null || value.equals("null"))
            return DEFAULT_ENGINE;
        else
            return Enum.valueOf(BACKENDS.class, value.toUpperCase());
    }



    public static void loadLogger () {
        InputStream stream = ConfigLoader.loadConfigStream("log4j.properties");
        PropertyConfigurator.configure(stream);
        jlog.info("Done loading logging framework Log4j!");
    }



    @Deprecated
    public static void loadLog4jLogger () {
        /** loadSubTypes log4j configuration file programmatically */
        Properties log4j = new Properties();
        try {
            File f = new File(System.getProperty("user.dir"),
                    "log4j.properties");
            if (f.exists()) {
                log4j.load(new FileInputStream(f));
                PropertyConfigurator.configure(log4j);
                jlog.info(
                        "using local logging properties file ({}) to configure logging system",
                        "./log4j.properties");
                return;
            }
        }
        catch (Exception e) {
            // do nothing
        }
        loadClassLogger();
    }


    @Deprecated
    private static void loadClassLogger () {
        Properties log4j = new Properties();
        jlog.info("using class path logging properties file to configure logging system");

        try {
            log4j.load(KustvaktConfiguration.class.getClassLoader()
                    .getResourceAsStream("log4j.properties"));
        }
        catch (IOException e) {
            // do nothing
        }

        PropertyConfigurator.configure(log4j);
        jlog.warn("No logger properties detected. Using default logger properties");
    }

    public enum BACKENDS {
        NEO4J, LUCENE
    }

}

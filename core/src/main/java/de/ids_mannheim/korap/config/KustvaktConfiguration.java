package de.ids_mannheim.korap.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.log4j.PropertyConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.ids_mannheim.korap.exceptions.KustvaktException;
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
    private ArrayList<String> foundries;
    private ArrayList<String> layers;
    
    private String baseURL;
    
    
    // deprec?!
    private final BACKENDS DEFAULT_ENGINE = BACKENDS.LUCENE;

	public KustvaktConfiguration (Properties properties) throws IOException, URISyntaxException {
        load(properties);
    }
	
    /**
     * loading of the properties and mapping to parameter variables
     * 
     * @param properties
     * @return
     * @throws IOException 
     * @throws URISyntaxException 
     * @throws KustvaktException 
     */
    protected void load (Properties properties)
            throws IOException, URISyntaxException {
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

        default_const = properties
                .getProperty("default.layer.constituent", "mate");
        default_dep = properties.getProperty("default.layer.dependency", "mate");
        default_lemma = properties.getProperty("default.layer.lemma", "tt");
        default_pos = properties.getProperty("default.layer.partOfSpeech", "tt");
        default_token = properties.getProperty("default.layer.orthography",
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
        
        sharedSecret = properties.getProperty("security.sharedSecret", "")
                .getBytes();
        adminToken = properties.getProperty("security.adminToken");

        longTokenTTL = TimeUtils.convertTimeToSeconds(properties.getProperty(
                "security.longTokenTTL", "100D"));
        tokenTTL = TimeUtils.convertTimeToSeconds(properties.getProperty(
                "security.tokenTTL", "72H"));
        shortTokenTTL = TimeUtils.convertTimeToSeconds(properties.getProperty(
                "security.shortTokenTTL", "3H"));

//        passcodeSaltField = properties.getProperty("security.passcode.salt",
//                "accountCreation");
        
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
     * @throws URISyntaxException 
     */
    public void setPropertiesAsStream (InputStream stream) throws URISyntaxException {
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

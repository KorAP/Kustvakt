package de.ids_mannheim.korap.config;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Pattern;

import de.ids_mannheim.korap.util.KrillProperties;
import de.ids_mannheim.korap.utils.TimeUtils;
import lombok.Getter;
import lombok.Setter;

/**
 * Describes configuration for Kustvakt by importing properties 
 * from kustvakt.conf file and setting default values if they are 
 * not configured.  
 * 
 * MH: if configuration class is extended, loadSubTypes method should be
 * overriden
 * 
 * @author hanl
 * @author margaretha
 */

@Setter
@Getter
public class KustvaktConfiguration {

    public static final Map<String, Object> KUSTVAKT_USER = new HashMap<>();

    private String vcInCaching;
    
    private String indexDir;
    private int port;
    // todo: make exclusive so that the containg languages can really
    // only be used then
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
    private int longTokenTTL;
    private int tokenTTL;
    private int shortTokenTTL;
    private String[] rewrite_strategies;

    private String default_pos;
    private String default_morphology;
    private String default_lemma;
    private String default_orthography;
    private String default_dep;
    private String default_const;
    private String apiWelcomeMessage;
    private String defaultStructureFoundry;
    private ArrayList<String> foundries;
    private ArrayList<String> layers;

    private String baseURL;
    private Properties properties;

    private Set<String> supportedVersions;
    private String currentVersion;

    // deprec?!
    private final BACKENDS DEFAULT_ENGINE = BACKENDS.LUCENE;
    
    // license patterns
    protected Pattern publicLicensePattern;
    protected Pattern freeLicensePattern;
    protected Pattern allLicensePattern;

    // random code generator
    private String secureRandomAlgorithm;
    private String messageDigestAlgorithm;

    // EM: metadata restriction
    // another variable might be needed to define which metadata fields are restricted 
    private boolean isMetadataRestricted = false;
    
    // EM: Maybe needed when we support pipe registration
    @Deprecated
    public static Map<String, String> pipes = new HashMap<>();
    
    public KustvaktConfiguration (Properties properties) throws Exception {
        load(properties);
//        readPipesFile("pipes");
        KrillProperties.setProp(properties);
    }

    public KustvaktConfiguration () {}
    
    public void loadBasicProperties (Properties properties) {
        port = new Integer(properties.getProperty("server.port", "8095"));
        baseURL = properties.getProperty("kustvakt.base.url", "/api/*");
        setSecureRandomAlgorithm(properties
                .getProperty("security.secure.random.algorithm", ""));
        setMessageDigestAlgorithm(
                properties.getProperty("security.md.algorithm", "MD5"));
    }
    
    /**
     * loading of the properties and mapping to parameter variables
     * 
     * @param properties
     * @return
     * @throws Exception
     */
    protected void load (Properties properties) throws Exception {
        loadBasicProperties(properties);

        apiWelcomeMessage = properties.getProperty("api.welcome.message", "Welcome to KorAP API!");
        currentVersion = properties.getProperty("current.api.version", "v1.0");

        String supportedVersions =
                properties.getProperty("supported.api.versions", "");
        
        this.supportedVersions = new HashSet<>();
        if (!supportedVersions.isEmpty()){
            List<String> versionArray = Arrays.asList(supportedVersions.split(" "));
            this.supportedVersions.addAll(versionArray);
        }
        this.supportedVersions.add(currentVersion);

        maxhits = new Integer(properties.getProperty("maxhits", "50000"));
        returnhits = new Integer(properties.getProperty("returnhits", "50000"));
        indexDir = properties.getProperty("krill.indexDir", "");
        
        // server options
        serverHost = String
                .valueOf(properties.getProperty("server.host", "localhost"));
        String queries = properties.getProperty("korap.ql", "");
        String[] qls = queries.split(",");
        queryLanguages = new ArrayList<>();
        for (String querylang : qls)
            queryLanguages.add(querylang.trim().toUpperCase());

        default_const =
                properties.getProperty("default.foundry.constituent", "corenlp");
        default_dep =
                properties.getProperty("default.foundry.dependency", "malt");
        default_lemma = properties.getProperty("default.foundry.lemma", "tt");
        default_morphology = properties.getProperty("default.foundry.morphology", "marmot");
        default_pos =
                properties.getProperty("default.foundry.partOfSpeech", "tt");
        default_orthography =
                properties.getProperty("default.foundry.orthography", "opennlp");
        defaultStructureFoundry =
                properties.getProperty("default.foundry.structure", "base");

        // security configuration
        inactiveTime = TimeUtils.convertTimeToSeconds(
                properties.getProperty("security.idleTimeoutDuration", "10M"));
        allowMultiLogIn = Boolean
                .valueOf(properties.getProperty("security.multipleLogIn"));

        loginAttemptNum = Long.parseLong(
                properties.getProperty("security.loginAttemptNum", "3"));
        loginAttemptTTL = TimeUtils.convertTimeToSeconds(
                properties.getProperty("security.authAttemptTTL", "30M"));

        loadFactor = Integer.valueOf(
                properties.getProperty("security.encryption.loadFactor", "15"));
        validationStringLength = Integer.valueOf(properties
                .getProperty("security.validation.stringLength", "150"));
        validationEmaillength = Integer.valueOf(properties
                .getProperty("security.validation.emailLength", "40"));

        sharedSecret =
                properties.getProperty("security.sharedSecret", "").getBytes();

        longTokenTTL = TimeUtils.convertTimeToSeconds(
                properties.getProperty("security.longTokenTTL", "100D"));
        tokenTTL = TimeUtils.convertTimeToSeconds(
                properties.getProperty("security.tokenTTL", "72H"));
        shortTokenTTL = TimeUtils.convertTimeToSeconds(
                properties.getProperty("security.shortTokenTTL", "3H"));
    }
    
    @Deprecated
    public void readPipesFile (String filename) throws IOException {
        File file = new File(filename);
        if (file.exists()) {
            BufferedReader br = new BufferedReader(
                    new InputStreamReader(new FileInputStream(file)));

            String line = null;
            while( (line=br.readLine())!=null ){
                String[] parts = line.split("\t");
                if (parts.length !=2){
                    continue;
                }
                else{
                    pipes.put(parts[0], parts[1]);
                }
            }
            br.close();
        }
    }
    

    /**
     * set properties
     * 
     * @param props
     * @throws IOException
     */
    // public void setProperties (Properties props) throws IOException
    // {
    // this.load(props);
    // }

    /**
     * properties can be overloaded after spring init
     * 
     * @param stream
     * @throws Exception
     */
    public void setPropertiesAsStream (InputStream stream) throws Exception {
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

    public enum BACKENDS {
        NEO4J, LUCENE
    }

}

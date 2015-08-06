package de.ids_mannheim.korap.config;

import de.ids_mannheim.korap.utils.KustvaktLogger;
import lombok.Getter;
import org.apache.log4j.PropertyConfigurator;
import org.slf4j.Logger;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * if configuration class is extended, loadSubTypes method should be overriden
 *
 * @author hanl
 * @date 05/02/2014
 */

@Getter
public class KustvaktConfiguration {

    private final Logger jlog = KustvaktLogger
            .initiate(KustvaktConfiguration.class);
    private String indexDir;
    private int port;
    // todo: make exclusive so that the containg languages can really only be used then
    private List<String> queryLanguages;

    private int maxhits;

    private int returnhits;
    private String serverHost;

    private String host;

    private URL issuer;

    private String default_pos;
    private String default_lemma;
    private String default_surface;
    private String default_dep;
    private String default_const;

    // deprec?!
    private final BACKENDS DEFAULT_ENGINE = BACKENDS.LUCENE;

    /**
     * loading of the properties and mapping to parameter variables
     *
     * @param korap
     * @return
     */
    protected Properties load(Properties korap) {
        String log4jconfig = korap
                .getProperty("log4jconfig", "log4j.properties");
        loadLog4jLogger(log4jconfig);
        maxhits = new Integer(korap.getProperty("maxhits", "50000"));
        returnhits = new Integer(korap.getProperty("returnhits", "50000"));
        indexDir = korap.getProperty("lucene.indexDir", "");
        port = new Integer(korap.getProperty("server.port", "8080"));
        // server options
        serverHost = String
                .valueOf(korap.getProperty("server.host", "localhost"));
        String queries = korap.getProperty("korap.ql", "");
        String[] qls = queries.split(",");
        queryLanguages = new ArrayList<>();
        for (String querylang : qls)
            queryLanguages.add(querylang.trim().toUpperCase());
        //        issuer = new URL(korap.getProperty("korap.issuer", ""));

        default_const = korap.getProperty("kustvakt.default.const", "mate");
        default_dep = korap.getProperty("kustvakt.default.dep", "mate");
        default_lemma = korap.getProperty("kustvakt.default.lemma", "tt");
        default_pos = korap.getProperty("kustvakt.default.pos", "tt");
        default_surface = korap
                .getProperty("kustvakt.default.opennlp", "opennlp");

        return korap;
    }

    /**
     * set properties
     *
     * @param props
     */
    public void setProperties(Properties props) {
        this.load(props);
    }

    /**
     * properties can be overloaded after spring init
     *
     * @param stream
     */
    public void setPropertiesAsStream(InputStream stream) {
        try {
            Properties p = new Properties();
            p.load(stream);
            this.load(p);
        }catch (IOException e) {
            e.printStackTrace();
        }

    }

    public BACKENDS chooseBackend(String value) {
        if (value == null || value.equals("null"))
            return DEFAULT_ENGINE;
        else
            return Enum.valueOf(BACKENDS.class, value.toUpperCase());
    }

    private void loadLog4jLogger(String log4jconfig) {
        /** loadSubTypes log4j configuration file programmatically */
        Properties log4j = new Properties();
        try {
            if (!log4jconfig.equals("")) {
                log4j.load(new FileInputStream(log4jconfig));
                PropertyConfigurator.configure(log4j);
                jlog.info(
                        "using local logging properties file ({}) to configure logging system",
                        log4jconfig);
            }else
                loadClassLogger();
        }catch (Exception e) {
            loadClassLogger();
        }
    }

    public void loadClassLogger() {
        Properties log4j = new Properties();
        jlog.info(
                "using class path logging properties file to configure logging system");

        try {
            log4j.load(KustvaktConfiguration.class.getClassLoader()
                    .getResourceAsStream("log4j.properties"));
        }catch (IOException e) {
            // do nothing
        }

        PropertyConfigurator.configure(log4j);
        jlog.warn(
                "No logger properties detected. Using default logger properties");
    }

    public enum BACKENDS {
        NEO4J, LUCENE
    }

}

package de.ids_mannheim.korap.config;

import de.ids_mannheim.korap.utils.KorAPLogger;
import lombok.Getter;
import org.apache.log4j.PropertyConfigurator;
import org.slf4j.Logger;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * if configuration class is extended, load method should be overriden
 * @author hanl
 * @date 05/02/2014
 */

@Getter
public class KustvaktConfiguration {

    private final Logger jlog = KorAPLogger
            .initiate(KustvaktConfiguration.class);

    // deprec?!
    private final BACKENDS DEFAULT_ENGINE = BACKENDS.LUCENE;
    private int maxhits;

    private int port;
    private int returnhits;
    private String serverHost;
    private String indexDir;

    private List<String> queryLanguages;
    private String host;

    private URL issuer;

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
        return korap;
    }

    public void setProperties(Properties props) {
        this.load(props);
    }

    public BACKENDS chooseBackend(String value) {
        if (value == null || value.equals("null"))
            return DEFAULT_ENGINE;
        else
            return Enum.valueOf(BACKENDS.class, value.toUpperCase());
    }

    private void loadLog4jLogger(String log4jconfig) {
        /** load log4j configuration file programmatically */
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

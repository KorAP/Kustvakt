package de.ids_mannheim.korap.utils;

import de.ids_mannheim.korap.config.ConfigLoader;
import lombok.Getter;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * @author hanl
 * @date 23/01/2014
 */
public class ServiceInfo {

    private static final ServiceInfo info = new ServiceInfo();

    public static final String UNKNOWN = "UNKNOWN";

    @Getter
    private String name;
    @Getter
    private String version;
    @Getter
    private String config;
    @Getter
    private String logger;
    @Getter
    private Boolean cacheable;
    @Getter
    private String cache_store;


    private ServiceInfo () {
        load();
    }


    private void load () {
        Properties props = new Properties();
        try {
            InputStream stream = getStream();
            props.load(stream);
            stream.close();
            this.version = (String) props.get("kustvakt.version");
            this.name = (String) props.get("kustvakt.name");
            this.config = (String) props.get("kustvakt.properties");
            this.logger = (String) props.get("kustvakt.logging");
            this.cacheable = Boolean.valueOf((String) props.get("kustvakt.cache"));
            this.cache_store = (String) props.get("kustvakt.cache_store");
        }
        catch (IOException e) {
            this.version = UNKNOWN;
            this.name = UNKNOWN;
            this.logger = UNKNOWN;
            this.config = UNKNOWN;
            this.cacheable = false;
            this.cache_store = UNKNOWN;
        }
    }


    private static InputStream getStream () throws IOException {
        String path = "kustvakt.info";
        InputStream stream = ConfigLoader.loadConfigStream(path);
        if (stream == null)
            throw new IOException("stream for resource " + path
                    + " could not be found...");
        return stream;
    }


    public static ServiceInfo getInfo () {
        return info;
    }
}

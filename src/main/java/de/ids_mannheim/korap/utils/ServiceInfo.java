package de.ids_mannheim.korap.utils;

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

    private static String UNKNOWN = "UNKNOWN";

    @Getter
    private String name;
    @Getter
    private String version;


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
        }
        catch (IOException e) {
            this.version = UNKNOWN;
            this.name = UNKNOWN;
        }
    }


    private static InputStream getStream () throws IOException {
        String path = "/kustvakt.info";
        InputStream stream = ServiceInfo.class.getResourceAsStream(path);
        if (stream == null)
            throw new IOException("stream for resource " + path
                    + " could not be found...");
        return stream;
    }


    public static ServiceInfo getInfo () {
        return info;
    }
}

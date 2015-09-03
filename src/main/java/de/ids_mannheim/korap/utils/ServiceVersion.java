package de.ids_mannheim.korap.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * @author hanl
 * @date 23/01/2014
 */
public class ServiceVersion {

    private ServiceVersion() {
    }

    public static String getAPIVersion() {
        String path = "/version.prop";
        InputStream stream = ServiceVersion.class.getResourceAsStream(path);
        if (stream == null)
            return "UNKNOWN";

        Properties props = new Properties();
        try {
            props.load(stream);
            stream.close();
            return (String) props.get("version");
        }catch (IOException e) {
            return "UNKNOWN";
        }

    }

}

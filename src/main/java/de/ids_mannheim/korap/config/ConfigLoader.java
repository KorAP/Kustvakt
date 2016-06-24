package de.ids_mannheim.korap.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Created by hanl on 08.06.16.
 */
public class ConfigLoader {

    private static final Logger jlog = LoggerFactory.getLogger(ConfigLoader.class);


    private ConfigLoader () {}


    public static InputStream loadConfigStream (String name) {
        InputStream stream = null;
        try {
            File f = new File(System.getProperty("user.dir"), name);

            if (f.exists()) {
                jlog.info("Loading config '" + name + "' from file!");
                stream = new FileInputStream(f);
            }
            else {
                jlog.info("Loading config '" + name + "' from classpath!");
                stream = ConfigLoader.class.getClassLoader().getResourceAsStream(
                        name);
            }
        }
        catch (IOException e) {
            // do nothing
        }
        if (stream == null)
            throw new RuntimeException("Config file '"+name+"' could not be loaded ...");
        return stream;
    }


    public static Properties loadProperties (String name) throws IOException {
        Properties p = new Properties();
        p.load(loadConfigStream(name));
        return p;
    }

}

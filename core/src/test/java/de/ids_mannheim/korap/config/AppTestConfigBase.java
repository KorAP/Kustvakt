package de.ids_mannheim.korap.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppTestConfigBase {

    private static String mainConfigurationFile = "kustvakt-test.conf";

    @Bean(name = ContextHolder.KUSTVAKT_CONFIG)
    public KustvaktConfiguration getConfig () {
        
        KustvaktConfiguration c = null;
        
        InputStream s = ConfigLoader.loadConfigStream(mainConfigurationFile);
        
        if (s != null){
            Properties p = new Properties();
            try {
                p.load(s);
                c = new KustvaktConfiguration(p);
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
        else {
            System.out.println("No properties found!");
            System.exit(-1);
        }
        return c;
    }

}

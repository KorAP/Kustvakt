package de.ids_mannheim.korap.config;

import net.sf.ehcache.CacheManager;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

/**
 * @author hanl
 * @date 03/02/2016
 */
public class KustvaktCacheManager {

    private static boolean loaded = false;

    public static void init() {
        if (!loaded) {
            System.out.println("LOADING EHCACHE CONFIG FROM FILE");
            InputStream in = null;
            try {
                in = new FileInputStream(new File("./ehcache.xml"));
            }catch (FileNotFoundException e) {
                e.printStackTrace();
            }

            if (in == null) {
                System.out.println("LOADING EHCACHE FROM CLASSPATH");
                in = KustvaktCacheManager.class
                        .getResourceAsStream("ehcache.xml");
            }
            CacheManager.newInstance(in);
            loaded = true;
        }
    }
}

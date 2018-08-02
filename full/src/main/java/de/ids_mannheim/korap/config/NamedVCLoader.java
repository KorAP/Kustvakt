package de.ids_mannheim.korap.config;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import de.ids_mannheim.korap.KrillCollection;
import de.ids_mannheim.korap.web.SearchKrill;

@Component
public class NamedVCLoader {
    @Autowired
    private FullConfiguration config;
    @Autowired
    private SearchKrill searchKrill;
    
    private static Logger jlog = LogManager.getLogger(NamedVCLoader.class);

    public void loadVCToCache ()
            throws IOException {

        String dir = config.getNamedVCPath();
        File d = new File(dir);
        if (!d.isDirectory()) {
            throw new IOException("Directory " + dir + " is not valid");
        }

        for (File file : d.listFiles()) {
            if (!file.exists()) {
                throw new IOException("File " + file + " is not found.");
            }
            else if (!file.getName().endsWith(".jsonld")) {
                throw new IOException("File " + file
                        + " is not allowed. Filename must ends with .jsonld");
            }

            long start = System.currentTimeMillis();
            String json = FileUtils.readFileToString(file, "utf-8");
            KrillCollection collection = new KrillCollection(json);
            collection.setIndex(searchKrill.getIndex());
            
            String filename = file.getName();
            filename = filename.substring(0,filename.length()-7);
            if (collection != null) {
                collection.storeInCache(filename);
            }
            long end = System.currentTimeMillis();
            jlog.debug(filename + " duration: " + (end - start));
            jlog.debug("memory cache: "
                    + KrillCollection.cache.calculateInMemorySize());
        }
    }
}

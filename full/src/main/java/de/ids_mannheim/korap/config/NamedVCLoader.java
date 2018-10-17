package de.ids_mannheim.korap.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import de.ids_mannheim.korap.KrillCollection;
import de.ids_mannheim.korap.constant.VirtualCorpusType;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.service.VirtualCorpusService;
import de.ids_mannheim.korap.util.QueryException;
import de.ids_mannheim.korap.web.SearchKrill;

@Component
public class NamedVCLoader {
    @Autowired
    private FullConfiguration config;
    @Autowired
    private SearchKrill searchKrill;
    @Autowired
    private VirtualCorpusService vcService;

    private static Logger jlog = LogManager.getLogger(NamedVCLoader.class);

    public void loadVCToCache (String filename, String filePath)
            throws IOException, QueryException, KustvaktException {

        InputStream is = NamedVCLoader.class.getResourceAsStream(filePath);
        String json = IOUtils.toString(is, "utf-8");
        if (json != null) {
            cacheVC(json, filename);
            vcService.storeVC(filename, VirtualCorpusType.SYSTEM, json, null,
                    null, null, true, "system");
        }
    }

    public void loadVCToCache ()
            throws IOException, QueryException, KustvaktException {

        String dir = config.getNamedVCPath();
        if (dir.isEmpty()) return;
        
        File d = new File(dir);
        if (!d.isDirectory()) {
            throw new IOException("Directory " + dir + " is not valid");
        }

        for (File file : d.listFiles()) {
            if (!file.exists()) {
                throw new IOException("File " + file + " is not found.");
            }

            String filename = file.getName();
            String json = readFile(file, filename);
            if (json != null) {
                cacheVC(json, filename);
                vcService.storeVC(filename, VirtualCorpusType.SYSTEM, json, null,
                        null, null, true, "system");
            }
        }
    }

    private String readFile (File file, String filename) throws IOException {
        String json = null;
        long start = System.currentTimeMillis();
        if (filename.endsWith(".jsonld")) {
            filename = filename.substring(0, filename.length() - 7);
            json = FileUtils.readFileToString(file, "utf-8");
        }
        else if (filename.endsWith(".jsonld.gz")) {
            filename = filename.substring(0, filename.length() - 10);
            GZIPInputStream gzipInputStream =
                    new GZIPInputStream(new FileInputStream(file));
            ByteArrayOutputStream bos = new ByteArrayOutputStream(512);
            bos.write(gzipInputStream);
            json = bos.toString("utf-8");
            bos.close();
        }
        else {
            System.err.println("File " + filename
                    + " is not allowed. Filename must ends with .jsonld or .jsonld.gz");
        }
        long end = System.currentTimeMillis();
        jlog.debug("READ " + filename + " duration: " + (end - start));
        return json;
    }

    private void cacheVC (String json, String filename)
            throws IOException, QueryException {
        long start, end;
        start = System.currentTimeMillis();

        KrillCollection collection = new KrillCollection(json);
        collection.setIndex(searchKrill.getIndex());

        if (collection != null) {
            collection.storeInCache(filename);
        }
        end = System.currentTimeMillis();
        jlog.info(filename + " caching duration: " + (end - start));
        jlog.debug("memory cache: "
                + KrillCollection.cache.calculateInMemorySize());
    }

}

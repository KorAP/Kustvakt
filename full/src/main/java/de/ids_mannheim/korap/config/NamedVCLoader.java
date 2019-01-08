package de.ids_mannheim.korap.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;

import javax.management.RuntimeErrorException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import de.ids_mannheim.korap.KrillCollection;
import de.ids_mannheim.korap.constant.VirtualCorpusType;
import de.ids_mannheim.korap.entity.VirtualCorpus;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.service.VirtualCorpusService;
import de.ids_mannheim.korap.util.QueryException;
import de.ids_mannheim.korap.web.SearchKrill;

@Component
public class NamedVCLoader implements Runnable{
    @Autowired
    private FullConfiguration config;
    @Autowired
    private SearchKrill searchKrill;
    @Autowired
    private VirtualCorpusService vcService;

    public static Logger jlog = LogManager.getLogger(NamedVCLoader.class);
    public static boolean DEBUG = false;

    @Override
    public void run () {
        try {
            loadVCToCache();
        }
        catch (IOException | QueryException | KustvaktException e) {
//            e.printStackTrace();
            throw new RuntimeErrorException(new Error(e.getMessage(), e.getCause()));
        }
    }
    
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
            String[] strArr = readFile(file, filename);
            filename = strArr[0];
            String json = strArr[1];
            if (json != null) {
                cacheVC(json, filename);
                try {
                    VirtualCorpus vc = vcService.searchVCByName("system",
                            filename, "system");
                    if (vc != null) {
                        if (DEBUG) {
                            jlog.debug("Delete existing vc: " + filename);
                        }
                        vcService.deleteVC("system", vc.getId());
                    }
                }
                catch (KustvaktException e) {
                    // ignore
                }
                vcService.storeVC(filename, VirtualCorpusType.SYSTEM, json,
                        null, null, null, true, "system");
            }
        }
    }

    private String[] readFile (File file, String filename)
            throws IOException, KustvaktException {
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
        if (DEBUG) {
            jlog.debug("READ " + filename + " duration: " + (end - start));
        }

        return new String[] { filename, json };
    }

    private void cacheVC (String json, String filename)
            throws IOException, QueryException {
        long start, end;
        start = System.currentTimeMillis();

        KrillCollection collection = new KrillCollection(json);
        collection.setIndex(searchKrill.getIndex());

        jlog.info("Store {} in cache ", filename);
        if (collection != null) {
            collection.storeInCache(filename);
        }
        end = System.currentTimeMillis();
        jlog.info("{} caching duration: {}", filename, (end - start));
        if (DEBUG) {
            jlog.debug("memory cache: "
                    + KrillCollection.cache.calculateInMemorySize());
        }
    }
}

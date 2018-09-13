package de.ids_mannheim.korap.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import de.ids_mannheim.korap.KrillCollection;
import de.ids_mannheim.korap.constant.VirtualCorpusType;
import de.ids_mannheim.korap.dao.VirtualCorpusDao;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.exceptions.StatusCodes;
import de.ids_mannheim.korap.service.VirtualCorpusService;
import de.ids_mannheim.korap.user.User.CorpusAccess;
import de.ids_mannheim.korap.util.QueryException;
import de.ids_mannheim.korap.web.SearchKrill;

@Component
public class NamedVCLoader {
    @Autowired
    private FullConfiguration config;
    @Autowired
    private SearchKrill searchKrill;
    @Autowired
    private VirtualCorpusDao vcDao;
    @Autowired
    private VirtualCorpusService vcService;
    
    private static Logger jlog = LogManager.getLogger(NamedVCLoader.class);

    public void loadVCToCache () throws IOException, QueryException, KustvaktException {

        String dir = config.getNamedVCPath();
        File d = new File(dir);
        if (!d.isDirectory()) {
            throw new IOException("Directory " + dir + " is not valid");
        }

        for (File file : d.listFiles()) {
            if (!file.exists()) {
                throw new IOException("File " + file + " is not found.");
            }

            long start, end;
            String json;
            String filename = file.getName();

            if (file.getName().endsWith(".jsonld")) {
                filename = filename.substring(0, filename.length() - 7);
                start = System.currentTimeMillis();
                json = FileUtils.readFileToString(file, "utf-8");
                end = System.currentTimeMillis();
            }
            else if (filename.endsWith(".jsonld.gz")) {
                filename = filename.substring(0, filename.length() - 10);
                start = System.currentTimeMillis();

                GZIPInputStream gzipInputStream =
                        new GZIPInputStream(new FileInputStream(file));
                ByteArrayOutputStream bos = new ByteArrayOutputStream(512);
                bos.write(gzipInputStream);
                json = bos.toString("utf-8");
                bos.close();
                end = System.currentTimeMillis();
            }
            else {
                System.err.println("File " + filename
                        + " is not allowed. Filename must ends with .jsonld or .jsonld.gz");
                continue;
            }
            jlog.debug(
                    "READ " + filename + " duration: " + (end - start));

            cacheVC(json, filename);
            storeVC(filename, json);
        }
    }
    
    private void cacheVC (String json, String filename) throws IOException, QueryException {
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

    private void storeVC (String name, String koralQuery) throws KustvaktException {
        if (!VirtualCorpusService.wordPattern.matcher(name).matches()) {
            throw new KustvaktException(StatusCodes.INVALID_ARGUMENT,
                    "Virtual corpus name must only contains letters, numbers, "
                            + "underscores, hypens and spaces",
                    name);
        }
        CorpusAccess requiredAccess = vcService.determineRequiredAccess(koralQuery);
        vcDao.createVirtualCorpus(name, VirtualCorpusType.SYSTEM,
                requiredAccess, koralQuery, null,
                null, null, true, "system");
    }
}

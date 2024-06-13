package de.ids_mannheim.korap.init;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.zip.GZIPInputStream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import de.ids_mannheim.korap.cache.VirtualCorpusCache;
import de.ids_mannheim.korap.config.FullConfiguration;
import de.ids_mannheim.korap.constant.QueryType;
import de.ids_mannheim.korap.constant.ResourceType;
import de.ids_mannheim.korap.entity.QueryDO;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.exceptions.StatusCodes;
import de.ids_mannheim.korap.service.QueryService;
import de.ids_mannheim.korap.util.QueryException;
import de.ids_mannheim.korap.web.SearchKrill;

/**
 * <p>Loads predefined virtual corpora at server start up and caches
 * them, if the VC have not been cached before. If there are changes
 * in the index, the cache will be updated.
 * </p>
 * 
 * <p>
 * All predefined VC are set as SYSTEM VC. The filenames are used as
 * VC names. Acceptable file extensions are .jsonld.gz or .jsonld. The
 * VC should be located at the folder indicated by
 * <em>krill.namedVC</em>
 * specified in kustvakt.conf.
 * </p>
 * 
 * @author margaretha
 *
 */
@Component
public class NamedVCLoader implements Runnable {
    @Autowired
    private FullConfiguration config;
    @Autowired
    private SearchKrill searchKrill;
    @Autowired
    private QueryService vcService;

    public static Logger jlog = LogManager.getLogger(NamedVCLoader.class);
    public static boolean DEBUG = false;

    @Override
    public void run () {
        try {
            loadVCToCache();
        }
        catch (IOException | QueryException e) {
            //            e.printStackTrace();
            throw new RuntimeException(e.getMessage(), e.getCause());
        }
    }

    public void loadVCToCache (String filename, String filePath)
            throws IOException, QueryException, KustvaktException {
        loadVCToCache(filename,filePath,null);
    }
    
    /**
     * Used for testing
     * 
     * @param filename
     * @param filePath
     * @throws IOException
     * @throws QueryException
     * @throws KustvaktException
     */
    public void loadVCToCache (String filename, String filePath, String json)
            throws IOException, QueryException {

        if (json==null || json.isEmpty()) {
            InputStream is = NamedVCLoader.class.getResourceAsStream(filePath);
            json = IOUtils.toString(is, "utf-8");
        }
        processVC(filename, json);
    }

    public void loadVCToCache () throws IOException, QueryException {

        String dir = config.getNamedVCPath();
        if (dir.isEmpty())
            return;

        File d = new File(dir);
        if (!d.isDirectory()) {
            throw new IOException("Directory " + dir + " is not valid");
        }

        jlog.info(Arrays.toString(d.list()));

        for (File file : d.listFiles()) {
            if (!file.exists()) {
                throw new IOException("File " + file + " is not found.");
            }

            String filename = file.getName();
            String[] strArr = readFile(file, filename);
            filename = strArr[0];
            String json = strArr[1];
            if (json != null) {
                processVC(filename, json);
            }
        }
    }
    
    /**
     * Stores and caches VC if the given VC does not exist.
     * Updates VC in the database and re-caches it, if the given VC exists.
     * Updates VC if there is any change in the index.
     * 
     * In this method, it will be checked if
     * <ol>
     *  <li> VC exists in the database</li>
     *  <li> VC exists in the cache </li>
     *  <li> KoralQuery of the given VC differs from an existing VC with
     * the same id. </li>
     *  <li> Index has been changed</li>
     * </ol>
     * 
     * Koral Query
     * 
     * @param vcId
     * @param json
     * @throws IOException
     * @throws QueryException
     */
    private void processVC (String vcId, String json)
            throws IOException, QueryException {
        boolean updateCache = false;
        try {
            // if VC exists in the DB
            QueryDO existingVC = vcService.searchQueryByName("system", vcId, "system",
                    QueryType.VIRTUAL_CORPUS);
            
            String koralQuery = existingVC.getKoralQuery();
            // if existing VC is different from input
            if (json.hashCode() != koralQuery.hashCode()) {
                updateCache = true;
                // updateVCinDB
                storeVCinDB(vcId, json, existingVC);
            }
        }
        catch (KustvaktException e) {
            // VC doesn't exist in the DB
            if (e.getStatusCode() == StatusCodes.NO_RESOURCE_FOUND) {
                storeVCinDB(vcId, json, null);
            }
            else {
                throw new RuntimeException(e);
            }    
        }
        
        cacheVC(vcId, json, updateCache);
    }

    private String[] readFile (File file, String filename) throws IOException {
        String json = null;
        long start = System.currentTimeMillis();
        if (filename.endsWith(".jsonld")) {
            filename = filename.substring(0, filename.length() - 7);
            json = FileUtils.readFileToString(file, "utf-8");
        }
        else if (filename.endsWith(".jsonld.gz")) {
            filename = filename.substring(0, filename.length() - 10);
            GZIPInputStream gzipInputStream = new GZIPInputStream(
                    new FileInputStream(file));
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

    /**
     * Caches the given VC if the VC is not found in cache and updates
     * the VC if it exists and there are changes in the index.
     * 
     * @param vcId
     *            vc-name
     * @param koralQuery
     * @throws IOException
     * @throws QueryException
     * @throws KustvaktException 
     */
    private void cacheVC (String vcId, String koralQuery, boolean updateVC)
            throws IOException, QueryException {
        config.setVcInCaching(vcId);
        if (updateVC) {
            jlog.info("Updating {} in cache ", vcId);
            VirtualCorpusCache.delete(vcId);
        }
        else if (VirtualCorpusCache.contains(vcId)) {
            jlog.info("Checking {} in cache ", vcId);
            
        }
        else {
            jlog.info("Storing {} in cache ", vcId);
        }

        long start, end;
        start = System.currentTimeMillis();
        VirtualCorpusCache.store(vcId, searchKrill.getIndex());
        end = System.currentTimeMillis();
        jlog.info("Duration : {}", (end - start));
        config.setVcInCaching("");
    }

    /**
     * Stores new VC or updates existing VC
     * 
     * @param vcId
     * @param koralQuery
     */
    private void storeVCinDB (String vcId, String koralQuery, QueryDO existingVC) {
        try {
            String info = (existingVC == null) ? "Storing" : "Updating";
            jlog.info("{} {} in database ", info, vcId);
            
            vcService.storeQuery(existingVC, "system", vcId, ResourceType.SYSTEM,
                    QueryType.VIRTUAL_CORPUS, koralQuery, null, null, null,
                    true, "system", null, null);
        }
        catch (KustvaktException e) {
            throw new RuntimeException(e);
        }
    }
}

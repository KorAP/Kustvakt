package de.ids_mannheim.korap.annotation;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.ids_mannheim.korap.dao.AnnotationDao;
import de.ids_mannheim.korap.dao.ResourceDao;
import de.ids_mannheim.korap.entity.AnnotationLayer;
import de.ids_mannheim.korap.entity.Resource;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.service.QueryService;
import de.ids_mannheim.korap.user.User.CorpusAccess;

/**
 * Parser for extracting data from resources.json listing virtual corpora 
 * available through KorapSRU.
 * 
 * @author margaretha
 *
 */
@Component
public class ResourceParser {
    private Logger log = LogManager.getLogger(ResourceParser.class);

    @Autowired
    private ResourceDao resourceDao;
    @Autowired
    private AnnotationDao annotationDao;
    @Autowired
    private QueryService queryService;

    public static String RESOURCE_FILE = "resources.json";
    public static ObjectMapper mapper = new ObjectMapper();

    public void run () throws IOException, KustvaktException {
        InputStream is = null;
        File f = new File("data/"+RESOURCE_FILE);
        if (f.exists()) {
            is = new FileInputStream(f);
        }
        else {
            is = ResourceParser.class.getClassLoader()
                    .getResourceAsStream("data/"+RESOURCE_FILE);
        }

        JsonNode node = mapper.readTree(is);
        for (JsonNode resource : node) {
            String resourceId = resource.at("/id").asText();
            String pid = resource.at("/pid").asText();
            String deTitle = resource.at("/de_title").asText();
            String enTitle = resource.at("/en_title").asText();
            String enDescription = resource.at("/en_description").asText(); 
            String institution = resource.at("/institution").asText();
            String requiredAccess = resource.at("/required_access").asText();
            String corpusQuery = resource.at("/corpus_query").asText();
            
            if (requiredAccess.isEmpty()){
				if (!corpusQuery.isEmpty()) {
					String koralQuery = queryService
							.serializeCorpusQuery(corpusQuery);
					// assume all vc are not cached and use the given koralQuery
					// for cached-vc, the koralQuery should contain referTo
					CorpusAccess access = queryService.determineRequiredAccess(
							false, resourceId, koralQuery);
					requiredAccess = access.name();
				}
            }
            Set<AnnotationLayer> layers = parseLayers(resource.at("/layers"));
            try {
                Resource r = resourceDao.retrieveResource(resourceId);
				if (r == null) {
					resourceDao.createResource(resourceId, pid, deTitle,
							enTitle, enDescription, layers, institution,
							corpusQuery, requiredAccess);
				}
				else {
					resourceDao.updateResource(r, pid, deTitle, enTitle,
							enDescription, layers, institution, corpusQuery,
							requiredAccess);
				}
            }
            catch (Exception e) {
                log.warn("Failed creating resource: " + e.getMessage());
            }
        }
    }

    private Set<AnnotationLayer> parseLayers (JsonNode layers) {
        Set<AnnotationLayer> layerSet = new HashSet<>(layers.size());
        for (JsonNode layer : layers) {
            String[] codes = layer.asText().split("/");
            AnnotationLayer annotationLayer = annotationDao
                    .retrieveAnnotationLayer(codes[0], codes[1]);
            layerSet.add(annotationLayer);
        }
        return layerSet;
    }
}

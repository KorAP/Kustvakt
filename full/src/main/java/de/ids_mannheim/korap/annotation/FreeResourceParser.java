package de.ids_mannheim.korap.annotation;

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

@Component
public class FreeResourceParser {
    private Logger log = LogManager.getLogger(FreeResourceParser.class);

    @Autowired
    private ResourceDao resourceDao;
    @Autowired
    private AnnotationDao annotationDao;

    public static String FREE_RESOURCE_FILE = "free-resources.json";
    public static ObjectMapper mapper = new ObjectMapper();

    public void run () throws IOException, KustvaktException {
        InputStream is = FreeResourceParser.class.getClassLoader()
                .getResourceAsStream(FREE_RESOURCE_FILE);
        JsonNode node = mapper.readTree(is);
        for (JsonNode resource : node) {
            String resourceId = resource.at("/id").asText();
//            log.debug(resourceId);
            Set<AnnotationLayer> layers = parseLayers(resource.at("/layers"));
            try {
                Resource r = resourceDao.retrieveResource(resourceId);
                if (r == null) {
                    resourceDao.createResource(resource.at("/id").asText(),
                            resource.at("/de_title").asText(),
                            resource.at("/en_title").asText(),
                            resource.at("/en_description").asText(), layers);
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
            AnnotationLayer annotationLayer =
                    annotationDao.retrieveAnnotationLayer(codes[0], codes[1]);
            layerSet.add(annotationLayer);
        }
        return layerSet;
    }
}

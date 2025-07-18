package de.ids_mannheim.korap.dto.converter;

import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import de.ids_mannheim.korap.dto.ResourceDto;
import de.ids_mannheim.korap.entity.AnnotationLayer;
import de.ids_mannheim.korap.entity.Resource;

/**
 * ResourceConverter prepares data transfer objects (DTOs) from
 * {@link Resource} entities. DTO structure defines controllers
 * output, namely the structure of JSON objects in HTTP responses.
 * 
 * @author margaretha
 *
 */
@Component
public class ResourceConverter {

    public List<ResourceDto> convertToResourcesDto (List<Resource> resources) {
        List<ResourceDto> resourceDtoList = new ArrayList<ResourceDto>(
                resources.size());
        ResourceDto dto;
        Map<String, String> titles;
        HashMap<Integer, String> layers;
        for (Resource r : resources) {
            dto = new ResourceDto();
            String description = r.getEnglishDescription();
            if (description != null && !description.isEmpty()) {
            	dto.setDescription(description);
            }
            
            String pid = r.getPid();
            if (pid !=null && !pid.isEmpty()) {
            	dto.setResourceId(pid);
            }
            else {
            	dto.setResourceId(r.getId());
            }
            dto.setLanguages(new String[] { "deu" });

            titles = new HashMap<String, String>();
            titles.put("en", r.getEnglishTitle());
            titles.put("de", r.getGermanTitle());
            dto.setTitles(titles);

            layers = new HashMap<Integer, String>();
            String foundry, layer, code;
            for (AnnotationLayer annotationPair : r.getLayers()) {
                foundry = annotationPair.getFoundry().getCode();
                layer = annotationPair.getLayer().getCode();
                code = foundry + "/" + layer;
                layers.put(annotationPair.getId(), code);
            }
            dto.setLayers(layers);
            
			String encodedCQ = URLEncoder.encode(r.getCorpusQuery(),
					Charset.forName("utf-8"));
            dto.setLandingPage("https://korap.ids-mannheim.de?cq=" +
            		encodedCQ);
            dto.setInstitution(r.getInstitution());
            dto.setRequiredAccess(r.getRequiredAccess());

            resourceDtoList.add(dto);
        }

        return resourceDtoList;
    }
}

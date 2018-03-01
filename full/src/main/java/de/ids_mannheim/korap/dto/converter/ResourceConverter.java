package de.ids_mannheim.korap.dto.converter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import de.ids_mannheim.korap.dto.ResourceDto;
import de.ids_mannheim.korap.entity.AnnotationPair;
import de.ids_mannheim.korap.entity.Resource;

/**
 * ResourceConverter prepares data transfer objects (DTOs) from {@link Resource}
 * entities. DTO structure defines controllers output, namely the structure of 
 * JSON objects in HTTP responses.
 * 
 * @author margaretha
 *
 */
@Component
public class ResourceConverter {

    public List<ResourceDto> convertToResourcesDto (List<Resource> resources) {
        List<ResourceDto> resourceDtoList =
                new ArrayList<ResourceDto>(resources.size());
        ResourceDto dto;
        Map<String, String> titles;
        HashMap<Integer, String> layers;
        for (Resource r : resources) {
            dto = new ResourceDto();
            dto.setDescription(r.getEnglishDescription());
            dto.setResourceId(r.getId());
            dto.setLanguages(new String[] { "deu" });

            titles = new HashMap<String, String>();
            titles.put("en", r.getEnglishTitle());
            titles.put("de", r.getGermanTitle());
            dto.setTitles(titles);

            layers = new HashMap<Integer, String>();
            String foundry, layer, code;
            for (AnnotationPair annotationPair : r.getLayers()) {
                foundry = annotationPair.getAnnotation1().getCode();
                layer = annotationPair.getAnnotation2().getCode();
                code = foundry + "/" + layer;
                layers.put(annotationPair.getId(), code);
            }
            dto.setLayers(layers);

            resourceDtoList.add(dto);
        }

        return resourceDtoList;
    }
}

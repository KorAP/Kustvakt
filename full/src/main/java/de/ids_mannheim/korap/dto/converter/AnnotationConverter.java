package de.ids_mannheim.korap.dto.converter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import de.ids_mannheim.korap.dto.FoundryDto;
import de.ids_mannheim.korap.dto.FoundryDto.Layer;
import de.ids_mannheim.korap.dto.LayerDto;
import de.ids_mannheim.korap.entity.Annotation;
import de.ids_mannheim.korap.entity.AnnotationPair;

/**
 * AnnotationConverter prepares data transfer objects (DTOs) from
 * entities. The DTOs, for instance, are serialized into JSON in the
 * controller classes and then sent as the response entity.
 * 
 * @author margaretha
 *
 */
@Component
public class AnnotationConverter {

    /**
     * Returns layer descriptions in a list of {@link LayerDto}s.
     * 
     * @param pairs
     *            a list of {@link AnnotationPair}s
     * @return a list of {@link LayerDto}s
     */
    public List<LayerDto> convertToLayerDto (List<AnnotationPair> pairs) {
        List<LayerDto> layerDtos = new ArrayList<LayerDto>(pairs.size());
        LayerDto dto;
        String foundry, layer;
        for (AnnotationPair p : pairs) {
            dto = new LayerDto();
            dto.setId(p.getId());
            dto.setDescription(p.getDescription());

            foundry = p.getAnnotation1().getCode();
            dto.setFoundry(foundry);

            layer = p.getAnnotation2().getCode();
            dto.setLayer(layer);
            dto.setCode(foundry + "/" + layer);
            layerDtos.add(dto);
        }

        return layerDtos;
    }


    /**
     * Returns foundry description in {@link FoundryDto}s
     * 
     * @param pairs
     *            a list of {@link AnnotationPair}s
     * @param language
     * @return a list of {@link FoundryDto}s
     */
    public List<FoundryDto> convertToFoundryDto (List<AnnotationPair> pairs,
            String language) {
        List<FoundryDto> foundryDtos = new ArrayList<FoundryDto>(pairs.size());
        Map<String, List<AnnotationPair>> foundryMap = createFoundryMap(pairs);

        for (String key : foundryMap.keySet()) {
            List<AnnotationPair> foundries = foundryMap.get(key);
            List<Layer> layers = new ArrayList<Layer>(foundries.size());
            FoundryDto dto = null;

            for (AnnotationPair f : foundries) {
                if (dto == null) {
                    Annotation foundry = f.getAnnotation1();
                    dto = new FoundryDto();
                    if (language.equals("de")){
                        dto.setDescription(foundry.getGermanDescription());
                    }
                    else{
                        dto.setDescription(foundry.getDescription());
                    }
                    dto.setCode(foundry.getCode());
                }

                Annotation layer = f.getAnnotation2();
                Map<String, String> tags = new HashMap<>();
                for (Annotation value : f.getValues()) {
                    if (language.equals("de")){
                        tags.put(value.getCode(), value.getGermanDescription());
                    }
                    else{
                        tags.put(value.getCode(), value.getDescription());
                    }
                }

                Layer l = dto.new Layer();
                l.setCode(layer.getCode());
                
                if (language.equals("de")){
                    l.setDescription(layer.getGermanDescription());
                }
                else{
                    l.setDescription(layer.getDescription());
                }
                
                l.setTags(tags);
                layers.add(l);
            }

            dto.setLayers(layers);
            foundryDtos.add(dto);
        }

        return foundryDtos;
    }


    private Map<String, List<AnnotationPair>> createFoundryMap (
            List<AnnotationPair> pairs) {
        Map<String, List<AnnotationPair>> foundries =
                new HashMap<String, List<AnnotationPair>>();
        for (AnnotationPair p : pairs) {
            String foundryCode = p.getAnnotation1().getCode();
            if (foundries.containsKey(foundryCode)) {
                foundries.get(foundryCode).add(p);
            }
            else {
                List<AnnotationPair> foundryList =
                        new ArrayList<AnnotationPair>();
                foundryList.add(p);
                foundries.put(foundryCode, foundryList);
            }
        }

        return foundries;
    }
}

package de.ids_mannheim.korap.dto.converter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import de.ids_mannheim.korap.dto.FoundryDto;
import de.ids_mannheim.korap.dto.FoundryDto.Key;
import de.ids_mannheim.korap.dto.FoundryDto.Layer;
import de.ids_mannheim.korap.dto.LayerDto;
import de.ids_mannheim.korap.entity.Annotation;
import de.ids_mannheim.korap.entity.AnnotationKey;
import de.ids_mannheim.korap.entity.AnnotationLayer;

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
     *            a list of {@link AnnotationLayer}s
     * @return a list of {@link LayerDto}s
     */
    public List<LayerDto> convertToLayerDto (List<AnnotationLayer> pairs) {
        List<LayerDto> layerDtos = new ArrayList<LayerDto>(pairs.size());
        LayerDto dto;
        String foundry, layer;
        for (AnnotationLayer p : pairs) {
            dto = new LayerDto();
            dto.setId(p.getId());
            dto.setDescription(p.getDescription());

            foundry = p.getFoundry().getCode();
            dto.setFoundry(foundry);

            layer = p.getLayer().getCode();
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
     *            a list of {@link AnnotationLayer}s
     * @param language
     * @return a list of {@link FoundryDto}s
     */
    public List<FoundryDto> convertToFoundryDto (List<AnnotationLayer> pairs,
            String language) {
        List<FoundryDto> foundryDtos = new ArrayList<FoundryDto>(pairs.size());
        Map<String, List<AnnotationLayer>> foundryMap = createFoundryMap(pairs);

        for (String foundryCode : foundryMap.keySet()) {
            List<AnnotationLayer> foundries = foundryMap.get(foundryCode);
            List<Layer> layers = new ArrayList<Layer>(foundries.size());
            FoundryDto dto = null;

            for (AnnotationLayer f : foundries) {
                if (dto == null) {
                    Annotation foundry = f.getFoundry();
                    dto = new FoundryDto();
                    if (language.equals("de")) {
                        dto.setDescription(foundry.getGermanDescription());
                    }
                    else {
                        dto.setDescription(foundry.getDescription());
                    }
                    dto.setCode(foundry.getCode());
                }

                Annotation layer = f.getLayer();
                List<Key> keys = new ArrayList<>();

                for (AnnotationKey ak : f.getKeys()) {
                    Annotation a = ak.getKey();
                    Map<String, String> values = new HashMap<>();
                    Key key = dto.new Key(a.getCode());
                    if (language.equals("de")) {
                        key.setDescription(a.getGermanDescription());
                        for (Annotation v : ak.getValues()) {
                            values.put(v.getCode(), v.getGermanDescription());
                        }

                    }
                    else {
                        key.setDescription(a.getDescription());
                        for (Annotation v : ak.getValues()) {
                            values.put(v.getCode(), v.getDescription());
                        }
                    }
                    key.setValues(values);
                    keys.add(key);
                }

                Layer l = dto.new Layer();
                l.setCode(layer.getCode());

                if (language.equals("de")) {
                    l.setDescription(layer.getGermanDescription());
                }
                else {
                    l.setDescription(layer.getDescription());
                }

                l.setKeys(keys);
                layers.add(l);
            }

            dto.setLayers(layers);
            foundryDtos.add(dto);
        }

        return foundryDtos;
    }

    private Map<String, List<AnnotationLayer>> createFoundryMap (
            List<AnnotationLayer> pairs) {
        Map<String, List<AnnotationLayer>> foundries =
                new HashMap<String, List<AnnotationLayer>>();
        for (AnnotationLayer p : pairs) {
            String foundryCode = p.getFoundry().getCode();
            if (foundries.containsKey(foundryCode)) {
                foundries.get(foundryCode).add(p);
            }
            else {
                List<AnnotationLayer> foundryList =
                        new ArrayList<AnnotationLayer>();
                foundryList.add(p);
                foundries.put(foundryCode, foundryList);
            }
        }

        return foundries;
    }
}

package de.ids_mannheim.korap.dto.converter;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import de.ids_mannheim.korap.dto.LayerDto;
import de.ids_mannheim.korap.entity.AnnotationPair;

@Component
public class AnnotationConverter {

    public List<LayerDto> convertToLayerDto (List<AnnotationPair> layers) {
        List<LayerDto> layerDtos = new ArrayList<LayerDto>(layers.size());
        LayerDto dto;
        String foundry, layer;
        for (AnnotationPair l : layers) {
            dto = new LayerDto();
            dto.setId(l.getId());
            dto.setDescription(l.getEnglishDescription());

            foundry = l.getAnnotation1().getCode();
            dto.setFoundry(foundry);

            layer = l.getAnnotation2().getCode();
            dto.setLayer(layer);
            dto.setCode(foundry + "/" + layer);
            layerDtos.add(dto);
        }
        
        return layerDtos;
    }
}

package de.ids_mannheim.korap.dto.converter;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;

import de.ids_mannheim.korap.dto.VirtualCorpusDto;
import de.ids_mannheim.korap.entity.VirtualCorpus;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.utils.JsonUtils;

@Service
public class VirtualCorpusConverter {

    public VirtualCorpusDto createVirtualCorpusDto (VirtualCorpus vc,
            String statistics) throws KustvaktException {

        VirtualCorpusDto dto = new VirtualCorpusDto();
        dto.setId(vc.getId());
        dto.setName(vc.getName());
        dto.setCreatedBy(vc.getCreatedBy());
        dto.setAccess(vc.getRequiredAccess().name());
        dto.setStatus(vc.getStatus());
        dto.setDescription(vc.getDescription());
        dto.setType(vc.getType().displayName());

        JsonNode node = JsonUtils.readTree(statistics);
        int numberOfDoc = node.at("/documents").asInt();
        dto.setNumberOfDoc(numberOfDoc);

        return dto;

    }
}

package de.ids_mannheim.korap.dto.converter;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;

import de.ids_mannheim.korap.dto.VirtualCorpusDto;
import de.ids_mannheim.korap.entity.VirtualCorpus;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.utils.JsonUtils;

/**
 * VirtualCorpusConverter prepares data transfer objects (DTOs) from
 * {@link VirtualCorpus} entities. DTO structure defines controllers
 * output, namely the structure of JSON objects in HTTP responses.
 * 
 * @author margaretha
 *
 */
@Component
public class VirtualCorpusConverter {

    public VirtualCorpusDto createVirtualCorpusDto (VirtualCorpus vc,
            String statistics) throws KustvaktException {

        VirtualCorpusDto dto = new VirtualCorpusDto();
        dto.setId(vc.getId());
        dto.setName(vc.getName());
        dto.setCreatedBy(vc.getCreatedBy());
        dto.setRequiredAccess(vc.getRequiredAccess().name());
        dto.setStatus(vc.getStatus());
        dto.setDescription(vc.getDescription());
        dto.setType(vc.getType().displayName());
        
        dto.setQuery(vc.getQuery());
        dto.setQueryLanguage(vc.getQueryLanguage());
        
        JsonNode kq = JsonUtils.readTree(vc.getKoralQuery());
        dto.setKoralQuery(kq);
        
        if (statistics != null) {
            JsonNode node = JsonUtils.readTree(statistics);
            dto.setNumberOfDoc(node.at("/documents").asInt());
            dto.setNumberOfParagraphs(node.at("/paragraphs").asInt());
            dto.setNumberOfSentences(node.at("/sentences").asInt());
            dto.setNumberOfTokens(node.at("/tokens").asInt());
        }
        return dto;

    }
}

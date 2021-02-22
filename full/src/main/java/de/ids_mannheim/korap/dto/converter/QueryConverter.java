package de.ids_mannheim.korap.dto.converter;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;

import de.ids_mannheim.korap.dto.QueryDto;
import de.ids_mannheim.korap.entity.QueryDO;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.utils.JsonUtils;

/**
 * QueryConverter prepares data transfer objects (DTOs) from
 * {@link QueryDO} entities. DTO structure defines controllers
 * output, namely the structure of JSON objects in HTTP responses.
 * 
 * @author margaretha
 *
 */
@Component
public class QueryConverter {

    public QueryDto createQueryDto (QueryDO query,
            String statistics) throws KustvaktException {

        QueryDto dto = new QueryDto();
        dto.setId(query.getId());
        dto.setName(query.getName());
        dto.setCreatedBy(query.getCreatedBy());
        dto.setRequiredAccess(query.getRequiredAccess().name());
        dto.setStatus(query.getStatus());
        dto.setDescription(query.getDescription());
        dto.setType(query.getType().displayName());
        
        dto.setQuery(query.getQuery());
        dto.setQueryLanguage(query.getQueryLanguage());
        
        JsonNode kq = JsonUtils.readTree(query.getKoralQuery());
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

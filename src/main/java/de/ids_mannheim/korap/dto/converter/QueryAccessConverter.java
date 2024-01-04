package de.ids_mannheim.korap.dto.converter;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import de.ids_mannheim.korap.dto.QueryAccessDto;
import de.ids_mannheim.korap.entity.QueryAccess;

/**
 * QueryAccessConverter prepares data transfer objects (DTOs)
 * from {@link QueryAccess} entities. DTO structure defines
 * controllers output, namely the structure of JSON objects in HTTP
 * responses.
 * 
 * @author margaretha
 *
 */
@Component
public class QueryAccessConverter {

    public List<QueryAccessDto> createQueryAccessDto (
            List<QueryAccess> accessList) {
        List<QueryAccessDto> dtos = new ArrayList<>(accessList.size());
        for (QueryAccess access : accessList) {
            QueryAccessDto dto = new QueryAccessDto();
            dto.setAccessId(access.getId());
            dto.setCreatedBy(access.getCreatedBy());

            dto.setQueryId(access.getQuery().getId());
            dto.setQueryName(access.getQuery().getName());

            dto.setUserGroupId(access.getUserGroup().getId());
            dto.setUserGroupName(access.getUserGroup().getName());

            dtos.add(dto);
        }
        return dtos;
    }

}

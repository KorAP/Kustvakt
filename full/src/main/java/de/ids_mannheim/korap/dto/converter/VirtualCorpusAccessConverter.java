package de.ids_mannheim.korap.dto.converter;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import de.ids_mannheim.korap.dto.VirtualCorpusAccessDto;
import de.ids_mannheim.korap.entity.VirtualCorpusAccess;
/**
 * VirtualCorpusAccessConverter prepares data transfer objects (DTOs) from {@link VirtualCorpusAccess}
 * entities. DTO structure defines controllers output, namely the structure of 
 * JSON objects in HTTP responses.
 * 
 * @author margaretha
 *
 */
@Component
public class VirtualCorpusAccessConverter {

    public List<VirtualCorpusAccessDto> createVCADto (List<VirtualCorpusAccess> accessList) {
        List<VirtualCorpusAccessDto> dtos = new ArrayList<>(accessList.size());
        for (VirtualCorpusAccess access : accessList){
            VirtualCorpusAccessDto dto = new VirtualCorpusAccessDto();
            dto.setAccessId(access.getId());
            dto.setCreatedBy(access.getCreatedBy());
            
            dto.setVcId(access.getVirtualCorpus().getId());
            dto.setVcName(access.getVirtualCorpus().getName());
            
            dto.setUserGroupId(access.getUserGroup().getId());
            dto.setUserGroupName(access.getUserGroup().getName());
            
            dtos.add(dto);
        }
        return dtos;
    }
    
}

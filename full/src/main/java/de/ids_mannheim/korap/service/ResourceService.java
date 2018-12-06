package de.ids_mannheim.korap.service;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import de.ids_mannheim.korap.dao.ResourceDao;
import de.ids_mannheim.korap.dto.ResourceDto;
import de.ids_mannheim.korap.dto.converter.ResourceConverter;
import de.ids_mannheim.korap.entity.Resource;
import de.ids_mannheim.korap.web.controller.ResourceController;

/** ResourceService defines the logic behind {@link ResourceController}.
 * 
 * @author margaretha
 *
 */
@Service
public class ResourceService {

    public static Logger jlog = LogManager.getLogger(ResourceService.class);
    public static boolean DEBUG = false;

    @Autowired
    private ResourceDao resourceDao;
    @Autowired
    private ResourceConverter resourceConverter;

    public List<ResourceDto> getResourceDtos () {
        List<Resource> resources = resourceDao.getAllResources();
        List<ResourceDto> resourceDtos =
                resourceConverter.convertToResourcesDto(resources);
        if (DEBUG) {
            jlog.debug("/info " + resourceDtos.toString());
        }
        return resourceDtos;
    }

}

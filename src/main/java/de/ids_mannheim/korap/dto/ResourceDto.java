package de.ids_mannheim.korap.dto;

import java.util.Map;

import lombok.Getter;
import lombok.Setter;

/** Data transfer object for resource description (e.g. for KorapSRU). 
 * 
 * @author margaretha
 *
 */
@Setter
@Getter
public class ResourceDto {

    private String resourceId;
    private String germanTitle;
    private String englishTitle;
    private String description;
    private Map<Integer, String> layers;
    private Map<String, String> languages;


    @Override
    public String toString () {
        return "resourceId= " + resourceId + ", germanTitle= " + germanTitle
                + ", englishTitle= " + englishTitle + ", description= "
                + description + ", languages= " + languages + ", layers= "
                + layers;
    }
}

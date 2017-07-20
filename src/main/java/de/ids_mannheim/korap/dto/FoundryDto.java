package de.ids_mannheim.korap.dto;

import java.util.List;
import java.util.Map;

import lombok.Getter;
import lombok.Setter;

/**
 * Data transfer object for annotation descriptions (e.g. for
 * Kalamar).
 * 
 * @author margaretha
 * 
 */
@Getter
@Setter
public class FoundryDto {

    private String code;
    private String description;
    private List<Layer> layers;

    @Getter
    @Setter
    public class Layer {
        private String code;
        private String description;
        // EM: pairs of annotation values and their description
        private Map<String, String> tags;
    }
}

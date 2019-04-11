package de.ids_mannheim.korap.dto;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

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
@JsonInclude(Include.NON_EMPTY) // new fasterxml annotation, not used by current jersey version
//@JsonSerialize(include=Inclusion.NON_EMPTY) // old codehouse annotation, used by jersey
public class FoundryDto {

    private String code;
    private String description;
    private List<Layer> layers;

    @Getter
    @Setter
    @JsonInclude(Include.NON_EMPTY)
//    @JsonSerialize(include=Inclusion.NON_EMPTY) // old codehouse annotation used by jersey
    public class Layer {
        private String code;
        private String description;
        private Set<Key> keys;
    }

    @Getter
    @Setter
    @JsonInclude(Include.NON_EMPTY)
//    @JsonSerialize(include=Inclusion.NON_EMPTY) // old codehouse annotation used by jersey
    public class Key implements Comparable<Key>{

        private String code;
        private String description;
        private Map<String, String> values;

        public Key (String code) {
            this.code = code;
        }
        
        @Override
        public int compareTo (Key k) {
            return this.code.compareTo(k.code); 
        }
    }
}

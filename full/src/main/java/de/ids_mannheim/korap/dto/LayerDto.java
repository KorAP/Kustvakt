package de.ids_mannheim.korap.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * Data transfer object for layer description (e.g. for KorapSRU).
 * 
 * @author margaretha
 *
 */
@Getter
@Setter
public class LayerDto {

    private int id;
    private String code;
    private String layer;
    private String foundry;
    private String description;
}

package de.ids_mannheim.korap.resource;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * @author hanl
 * @date 19/06/2015
 */
public abstract class NodeProcessor {

    public abstract JsonNode process(JsonNode node);

}

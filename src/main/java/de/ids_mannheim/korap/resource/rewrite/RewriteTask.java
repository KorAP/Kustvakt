package de.ids_mannheim.korap.resource.rewrite;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;

/**
 * @author hanl
 * @date 30/06/2015
 */
@Getter
public abstract class RewriteTask {

    protected RewriteTask() {
    }

    public abstract JsonNode rewrite(KoralNode node);
}

package de.ids_mannheim.korap.rewrite;

import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.databind.JsonNode;

import de.ids_mannheim.korap.config.KustvaktConfiguration;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.user.User;
import de.ids_mannheim.korap.user.UserSettingProcessor;

/**
 * @author hanl, margaretha
 * @date 30/06/2015
 */
public class FoundryInject implements RewriteTask.IterableRewritePath {

    @Autowired
    protected LayerMapper mapper;

    @Override
    public KoralNode rewriteQuery (KoralNode koralNode, KustvaktConfiguration config,
            User user, double apiVersion) throws KustvaktException {

    	// EM: I don't know the purpose of the following code and it is not 
    	// tested
        if (koralNode.get("@type").equals("koral:span")) {
            if (!koralNode.isMissingNode("/wrap")) {
                koralNode = koralNode.at("/wrap");
                JsonNode term = rewriteQuery(koralNode, config, user, apiVersion).rawNode();
                koralNode.replaceAt("/wrap", term,
                        new RewriteIdentifier("koral:term", "replace", ""));
            }
        }
        else if (koralNode.get("@type").equals("koral:term")
                && !koralNode.has("foundry")) {
            String layer;
            if (koralNode.has("layer")) {
                layer = koralNode.get("layer");
            }
            else {
                layer = koralNode.get("key");
            }
            UserSettingProcessor settingProcessor = null;
            if (user != null) {
                settingProcessor = user.getUserSettingProcessor();
            }
            String foundry = mapper.findFoundry(layer, settingProcessor);
			if (foundry != null) {
				RewriteIdentifier ri = new RewriteIdentifier("foundry", null,
						"Default foundry has been added.");
				koralNode.set("foundry", foundry, ri);
			}
        }
        return koralNode;
    }

    @Override
    public String path () {
        return "query";
    }

    @Override
    public JsonNode rewriteResult (KoralNode node) {
        return null;
    }
}

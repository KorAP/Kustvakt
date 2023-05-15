package de.ids_mannheim.korap.rewrite;

import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.databind.JsonNode;

import de.ids_mannheim.korap.config.KustvaktConfiguration;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.rewrite.KoralNode.RewriteIdentifier;
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
    public KoralNode rewriteQuery (KoralNode node, KustvaktConfiguration config,
            User user) throws KustvaktException {
        
        if (node.get("@type").equals("koral:span")) {
            if (!node.isMissingNode("/wrap")){
                node = node.at("/wrap");
                JsonNode term = rewriteQuery(node, config, user).rawNode();
                node.replaceAt("/wrap", term, new RewriteIdentifier("koral:term", "replace"));
            }
        }
        else if (node.get("@type").equals("koral:term") && !node.has("foundry")) {
            String layer;
            if (node.has("layer")){
                layer = node.get("layer");
            }
            else{
                layer = node.get("key");
            }
            UserSettingProcessor settingProcessor = null;
            if (user!=null){
                settingProcessor = user.getUserSettingProcessor();
            }
            String foundry = mapper.findFoundry(layer, settingProcessor);
            if (foundry != null)
                node.put("foundry", foundry);
        }
        return node;
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

package de.ids_mannheim.korap.resource.rewrite;

import com.fasterxml.jackson.databind.JsonNode;
import de.ids_mannheim.korap.config.BeanConfiguration;
import de.ids_mannheim.korap.config.KustvaktConfiguration;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.interfaces.db.UserDataDbIface;
import de.ids_mannheim.korap.resource.LayerMapper;
import de.ids_mannheim.korap.user.User;

/**
 * @author hanl
 * @date 30/06/2015
 */
public class FoundryInject implements RewriteTask.IterableRewriteAt {

    @Override
    public JsonNode preProcess(KoralNode node, KustvaktConfiguration config,
            User user) throws KustvaktException {
        LayerMapper mapper;
        if (user != null && BeanConfiguration.hasContext()) {
            UserDataDbIface dao = BeanConfiguration.getBeans()
                    .getUserSettingsDao();
            mapper = new LayerMapper(config, dao.get(user));
        }else
            mapper = new LayerMapper(config);

        if (node.get("@type").equals("koral:term") && !node.has("foundry")) {
            String layer;
            if (node.has("layer"))
                layer = node.get("layer");
            else
                layer = node.get("key");
            String foundry = mapper.findFoundry(layer);
            node.put("foundry", foundry);
        }
        return node.rawNode();
    }

    @Override
    public String path() {
        return "query";
    }

    @Override
    public JsonNode postProcess(KoralNode node) {
        return null;
    }
}

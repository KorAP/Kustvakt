package de.ids_mannheim.korap.resource.rewrite;

import com.fasterxml.jackson.databind.JsonNode;
import de.ids_mannheim.korap.config.BeanInjectable;
import de.ids_mannheim.korap.config.BeansFactory;
import de.ids_mannheim.korap.config.ContextHolder;
import de.ids_mannheim.korap.config.KustvaktConfiguration;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.interfaces.db.UserDataDbIface;
import de.ids_mannheim.korap.resource.LayerMapper;
import de.ids_mannheim.korap.user.User;
import de.ids_mannheim.korap.user.UserSettings;
import edu.emory.mathcs.backport.java.util.Collections;

import java.util.Collection;

/**
 * @author hanl
 * @date 30/06/2015
 */
public class FoundryInject implements RewriteTask.IterableRewritePath,
        BeanInjectable {

    private Collection userdaos;


    public FoundryInject () {
        this.userdaos = Collections.emptyList();
    }


    @Override
    public JsonNode rewriteQuery (KoralNode node, KustvaktConfiguration config,
            User user) throws KustvaktException {
        LayerMapper mapper;
        // EM: do not use DB
//        if (user != null && !userdaos.isEmpty()) {
//            UserDataDbIface dao = BeansFactory.getTypeFactory()
//                    .getTypeInterfaceBean(userdaos, UserSettings.class);
//            mapper = new LayerMapper(config, dao.get(user));
//        }
//        else
            mapper = new LayerMapper(config);

        if (node.get("@type").equals("koral:term") && !node.has("foundry")) {
            String layer;
            if (node.has("layer"))
                layer = node.get("layer");
            else
                layer = node.get("key");
            String foundry = mapper.findFoundry(layer);
            if (foundry != null)
                node.put("foundry", foundry);
        }
        return node.rawNode();
    }


    @Override
    public String path () {
        return "query";
    }


    @Override
    public JsonNode rewriteResult (KoralNode node) {
        return null;
    }


    @Override
    public <T extends ContextHolder> void insertBeans (T beans) {
        this.userdaos = beans.getUserDataProviders();
    }
}

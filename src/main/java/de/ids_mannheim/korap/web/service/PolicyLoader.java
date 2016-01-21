package de.ids_mannheim.korap.web.service;

import de.ids_mannheim.korap.config.BeanConfiguration;
import de.ids_mannheim.korap.config.KustvaktConfiguration;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.handlers.CollectionDao;
import de.ids_mannheim.korap.handlers.ResourceDao;
import de.ids_mannheim.korap.interfaces.db.PersistenceClient;
import de.ids_mannheim.korap.interfaces.db.ResourceOperationIface;
import de.ids_mannheim.korap.resources.Corpus;
import de.ids_mannheim.korap.resources.Permissions;
import de.ids_mannheim.korap.security.ac.PolicyBuilder;
import de.ids_mannheim.korap.security.ac.PolicyDao;
import de.ids_mannheim.korap.security.ac.ResourceFinder;
import de.ids_mannheim.korap.security.ac.SecurityManager;
import de.ids_mannheim.korap.user.User;

import java.util.HashSet;
import java.util.Set;

/**
 * @author hanl
 * @date 15/01/2016
 */
public class PolicyLoader implements BootupInterface {

    @Override
    public void load() throws KustvaktException {
        if (BeanConfiguration.hasContext()) {
            PersistenceClient cl = BeanConfiguration.getBeans()
                    .getPersistenceClient();
            Set<ResourceOperationIface> ifaces = new HashSet<>();
            ifaces.add(new ResourceDao(cl));
            ifaces.add(new CollectionDao(cl));

            SecurityManager.setProviders(new PolicyDao(cl),
                    BeanConfiguration.getBeans().getEncryption(), ifaces);
            ResourceFinder.setProviders(new PolicyDao(cl));

            User user = User.UserFactory
                    .toUser(KustvaktConfiguration.KUSTVAKT_USER);
            PolicyBuilder builder = new PolicyBuilder(user);
            builder.addCondition("public");
            builder.setResources(new Corpus("GOE", user.getId()));
            builder.setPermissions(Permissions.PERMISSIONS.ALL);
            builder.create();

            // redundant if user is the user who created the condition for the resource
            //        try {
            //            ConditionManagement cm = new ConditionManagement(user);
            ////            cm.addUser(user.getUsername(), new PolicyCondition("public"), true);
            //        }catch (KustvaktException e) {
            //            e.printStackTrace();
            //        }
        }
    }

    @Override
    public int position() {
        return 1;
    }
}

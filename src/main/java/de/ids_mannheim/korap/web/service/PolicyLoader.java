package de.ids_mannheim.korap.web.service;

import de.ids_mannheim.korap.config.ContextHolder;
import de.ids_mannheim.korap.config.KustvaktConfiguration;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.resources.Corpus;
import de.ids_mannheim.korap.resources.Permissions;
import de.ids_mannheim.korap.security.ac.PolicyBuilder;
import de.ids_mannheim.korap.security.ac.ResourceFinder;
import de.ids_mannheim.korap.security.ac.SecurityManager;
import de.ids_mannheim.korap.user.User;

/**
 * @author hanl
 * @date 15/01/2016
 */
public class PolicyLoader implements BootableBeanInterface {

    @Override
    public void load (ContextHolder beans) throws KustvaktException {
        SecurityManager.overrideProviders(beans);
        ResourceFinder.overrideProviders(beans);

        User user = User.UserFactory
                .toUser(KustvaktConfiguration.KUSTVAKT_USER);
        KustvaktConfiguration config =beans.getConfiguration();
        PolicyBuilder builder = new PolicyBuilder(user);
//        builder.addCondition("public");
//        builder.setResources(new Corpus("GOE"));
//        builder.setPermissions(Permissions.Permission.READ);
//        builder.create();

        builder = new PolicyBuilder(user);
        builder.addCondition("public");
        builder.setResources(new Corpus(config.getDefaultVirtualCollectionId()));
        builder.setPermissions(Permissions.Permission.READ);
        builder.create();

//        KustvaktResource tt = new Foundry("tt");
//        tt.setName("TreeTagger");
//        tt.setDescription("todo ...");
//        builder = new PolicyBuilder(user);
//        builder.addCondition("public");
//        builder.setResources(tt);
//        builder.setPermissions(Permissions.Permission.READ);
//        builder.create();
    }


    @Override
    public Class<? extends BootableBeanInterface>[] getDependencies () {
        return new Class[] { UserLoader.class };
    }
}

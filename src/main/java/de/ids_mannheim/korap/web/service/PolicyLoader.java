package de.ids_mannheim.korap.web.service;

import de.ids_mannheim.korap.config.BeanConfiguration;
import de.ids_mannheim.korap.config.KustvaktConfiguration;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.resources.Corpus;
import de.ids_mannheim.korap.resources.Permissions;
import de.ids_mannheim.korap.security.ac.PolicyBuilder;
import de.ids_mannheim.korap.user.User;

/**
 * @author hanl
 * @date 15/01/2016
 */
public class PolicyLoader implements BootupInterface {

    @Override
    public void load() throws KustvaktException {
        if (BeanConfiguration.hasContext()) {
            User user = User.UserFactory
                    .toUser(KustvaktConfiguration.KUSTVAKT_USER);
            PolicyBuilder builder = new PolicyBuilder(user);
            builder.addCondition("public");
            builder.setResources(new Corpus("GOE"));
            builder.setPermissions(Permissions.Permission.READ);
            builder.create();
        }
    }

    @Override
    public Class<? extends BootupInterface>[] getDependencies() {
        return new Class[] { UserLoader.class };
    }
}

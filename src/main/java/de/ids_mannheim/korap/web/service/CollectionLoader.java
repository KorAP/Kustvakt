package de.ids_mannheim.korap.web.service;

import de.ids_mannheim.korap.config.BeanConfiguration;
import de.ids_mannheim.korap.config.KustvaktConfiguration;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.handlers.CollectionDao;
import de.ids_mannheim.korap.resources.Permissions;
import de.ids_mannheim.korap.resources.ResourceFactory;
import de.ids_mannheim.korap.resources.VirtualCollection;
import de.ids_mannheim.korap.security.ac.PolicyBuilder;
import de.ids_mannheim.korap.user.Attributes;
import de.ids_mannheim.korap.user.User;
import de.ids_mannheim.korap.utils.CollectionQueryBuilder3;

/**
 * @author hanl
 * @date 12/01/2016
 */
public class CollectionLoader implements BootupInterface {

    @Override
    public void load() throws KustvaktException {
        if (BeanConfiguration.hasContext()) {
            CollectionDao dao = new CollectionDao(
                    BeanConfiguration.getBeans().getPersistenceClient());

            int uid = (Integer) KustvaktConfiguration.KUSTVAKT_USER
                    .get(Attributes.ID);

            User user = User.UserFactory
                    .toUser(KustvaktConfiguration.KUSTVAKT_USER);

            //todo: load default collections!
            CollectionQueryBuilder3 bui = new CollectionQueryBuilder3();
            bui.addQuery("creationDate since 1775");

            VirtualCollection c1 = ResourceFactory
                    .createCollection("Weimarer Werke", bui.toJSON(), uid);
            c1.setDescription("Goethe-Werke in Weimar (seit 1775)");

            bui = new CollectionQueryBuilder3();
            bui.addQuery("textType = Aphorismus");

            VirtualCollection c2 = ResourceFactory
                    .createCollection("Aphorismen", bui.toJSON(), uid);
            c2.setDescription("Aphorismentexte Goethes");

            bui = new CollectionQueryBuilder3();
            bui.addQuery("title ~ \"Werther\"");

            VirtualCollection c3 = ResourceFactory
                    .createCollection("Werther", bui.toJSON(), uid);
            c3.setDescription("Goethe - Die Leiden des jungen Werther");

            PolicyBuilder b = new PolicyBuilder(user);
            b.setPermissions(Permissions.PERMISSIONS.ALL);
            b.setResources(c1, c2, c3);
            b.setConditions("public");
            b.create();
        }
    }

    @Override
    public int position() {
        return -1;
    }
}

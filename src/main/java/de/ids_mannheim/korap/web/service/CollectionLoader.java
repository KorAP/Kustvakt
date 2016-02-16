package de.ids_mannheim.korap.web.service;

import de.ids_mannheim.korap.config.BeanConfiguration;
import de.ids_mannheim.korap.config.KustvaktConfiguration;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.exceptions.StatusCodes;
import de.ids_mannheim.korap.resources.Permissions;
import de.ids_mannheim.korap.resources.VirtualCollection;
import de.ids_mannheim.korap.security.ac.PolicyBuilder;
import de.ids_mannheim.korap.user.Attributes;
import de.ids_mannheim.korap.user.User;
import de.ids_mannheim.korap.utils.CollectionQueryBuilder3;
import de.ids_mannheim.korap.utils.JsonUtils;

/**
 * @author hanl
 * @date 12/01/2016
 */
public class CollectionLoader implements BootupInterface {

    @Override
    public void load() throws KustvaktException {
        if (BeanConfiguration.hasContext()) {
            User user = User.UserFactory
                    .toUser(KustvaktConfiguration.KUSTVAKT_USER);

            //todo: load default collections!
            CollectionQueryBuilder3 bui = new CollectionQueryBuilder3();
            bui.addQuery("creationDate since 1775");

            VirtualCollection c1 = new VirtualCollection();
            c1.setName("Weimarer Werke");
            c1.addField(Attributes.QUERY, bui.toJSON());

            c1.setDescription("Goethe-Werke in Weimar (seit 1775)");

            bui = new CollectionQueryBuilder3();
            bui.addQuery("textType = Aphorismus");

            VirtualCollection c2 = new VirtualCollection();
            c2.setName("Aphorismen");
            c2.addField(Attributes.QUERY, bui.toJSON());
            c2.setDescription("Aphorismentexte Goethes");

            bui = new CollectionQueryBuilder3();
            bui.addQuery("title ~ \"Werther\"");

            VirtualCollection c3 = new VirtualCollection();
            c3.setName("Werther");
            c3.addField(Attributes.QUERY, bui.toJSON());
            c3.setDescription("Goethe - Die Leiden des jungen Werther");

            PolicyBuilder b = new PolicyBuilder(user);
            b.setPermissions(Permissions.Permission.READ);
            b.setResources(c1, c2, c3);
            b.setConditions("public");
            String result = b.create();

            if (JsonUtils.readTree(result).size() > 0)
                throw new KustvaktException(StatusCodes.REQUEST_INVALID,
                        "creating collections caused errors", result);
        }
    }

    @Override
    public Class<? extends BootupInterface>[] getDependencies() {
        return new Class[] { UserLoader.class };
    }
}

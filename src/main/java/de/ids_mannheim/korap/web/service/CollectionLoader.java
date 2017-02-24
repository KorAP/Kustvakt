package de.ids_mannheim.korap.web.service;

import de.ids_mannheim.korap.config.ContextHolder;
import de.ids_mannheim.korap.config.KustvaktConfiguration;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.exceptions.StatusCodes;
import de.ids_mannheim.korap.resources.Permissions;
import de.ids_mannheim.korap.resources.VirtualCollection;
import de.ids_mannheim.korap.security.ac.PolicyBuilder;
import de.ids_mannheim.korap.security.ac.ResourceFinder;
import de.ids_mannheim.korap.security.ac.SecurityManager;
import de.ids_mannheim.korap.user.User;
import de.ids_mannheim.korap.utils.KoralCollectionQueryBuilder;
import de.ids_mannheim.korap.utils.JsonUtils;

/**
 * @author hanl
 * @date 12/01/2016
 */
public class CollectionLoader implements BootableBeanInterface {

    @Override
    public void load (ContextHolder beans) throws KustvaktException {
        SecurityManager.overrideProviders(beans);
        ResourceFinder.overrideProviders(beans);

        User user = User.UserFactory
                .toUser(KustvaktConfiguration.KUSTVAKT_USER);

        KustvaktConfiguration config = beans.getConfiguration();
        
//        KoralCollectionQueryBuilder bui = new KoralCollectionQueryBuilder();
//        bui.with("creationDate since 1775 & corpusSigle=GOE");
//
//        VirtualCollection c1 = new VirtualCollection();
//        c1.setName("Weimarer Werke");
//
//        c1.setFields(bui.toJSON());
//
//        c1.setDescription("Goethe-Werke in Weimar (seit 1775)");
//
//        bui = new KoralCollectionQueryBuilder();
//        bui.with("textType=Aphorismus");
//
//        VirtualCollection c2 = new VirtualCollection();
//        c2.setName("Aphorismen");
//        c2.setFields(bui.toJSON());
//        c2.setDescription("Aphorismentexte Goethes");
//
//        bui = new KoralCollectionQueryBuilder();
//        bui.with("title ~ \"Werther\"");
//
//        VirtualCollection c3 = new VirtualCollection();
//        c3.setName("Werther");
//        c3.setFields(bui.toJSON());
//        c3.setDescription("Goethe - Die Leiden des jungen Werther");
        
        VirtualCollection c4 = new VirtualCollection();
        c4.setName(config.getDefaultVirtualCollectionName());
        c4.setDescription(config.getDefaultVirtualCollectionDescription());

        PolicyBuilder b = new PolicyBuilder(user);
        b.setPermissions(Permissions.Permission.READ);
        b.setResources(c4);
        b.setConditions("public");
        String result = b.create();

        if (JsonUtils.readTree(result).size() > 0)
            throw new KustvaktException(StatusCodes.REQUEST_INVALID,
                    "creating collections caused errors", result);
    }


    @Override
    public Class<? extends BootableBeanInterface>[] getDependencies () {
        return new Class[] { UserLoader.class };
    }
}

package de.ids_mannheim.korap.web.service;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;

import de.ids_mannheim.korap.config.ContextHolder;
import de.ids_mannheim.korap.config.KustvaktConfiguration;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.exceptions.StatusCodes;
import de.ids_mannheim.korap.resources.Corpus;
import de.ids_mannheim.korap.resources.Foundry;
import de.ids_mannheim.korap.resources.KustvaktResource;
import de.ids_mannheim.korap.resources.Layer;
import de.ids_mannheim.korap.resources.Permissions;
import de.ids_mannheim.korap.resources.VirtualCollection;
import de.ids_mannheim.korap.resources.Permissions.Permission;
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
        
        PolicyBuilder builder = new PolicyBuilder(user);
        String result = null;
		BufferedReader br;
		try {
			File f = new File(config.getPolicyConfig());
			br = new BufferedReader(new InputStreamReader(new FileInputStream(f)));
		} catch (FileNotFoundException e) {
			throw new KustvaktException("Policy config file: " + 
					config.getPolicyConfig() + " does not exists!",
					e.getCause(), 101);
		}
		String policy = null;
		String[] policyData = null;
		String type, id, name, description, condition;
		String[] permissions;
		try {
			while ((policy = br.readLine()) != null) {
				if (policy.startsWith("#") || policy.isEmpty()) continue;
				policyData = policy.split("\t");
				type = policyData[0];
				id = policyData[1];
				name = policyData[2];
				description = policyData[3];
				condition = policyData[4];
				permissions = policyData[5].split(",");

				Permission[] permissionArr = new Permission[permissions.length];
				for (int i = 0; i < permissions.length; i++) {
					if (permissions[i].equals("read")) {
						permissionArr[i] = Permission.READ;
					}
				}

				KustvaktResource resource = createResource(type, id, name, description);
				if (resource != null) {
					builder = new PolicyBuilder(user);
					builder.addCondition(condition);
					builder.setResources(resource);
					builder.setPermissions(permissionArr);
					result = builder.create();
					if (JsonUtils.readTree(result).size() > 0)
			            throw new KustvaktException(StatusCodes.REQUEST_INVALID,
			                    "creating collections caused errors", result);
				}
			}
			br.close();
		} catch (IOException e) {
			throw new KustvaktException("Failed creating virtual collections.", e.getCause(), 100);
		}
    }

    private KustvaktResource createResource(String type, String id, String name, String description) {

		KustvaktResource resource = null;
		if (type.equals("virtualcollection")) {
			resource = new VirtualCollection(id);
			if (!name.isEmpty()) {
				resource.setName(name);
			}
			if (!description.isEmpty()) {
				resource.setDescription(description);
			}
		}

		return resource;
	}


    @Override
    public Class<? extends BootableBeanInterface>[] getDependencies () {
        return new Class[] { UserLoader.class };
    }
}

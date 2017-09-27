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
import de.ids_mannheim.korap.resources.Corpus;
import de.ids_mannheim.korap.resources.Foundry;
import de.ids_mannheim.korap.resources.KustvaktResource;
import de.ids_mannheim.korap.resources.Layer;
import de.ids_mannheim.korap.resources.VirtualCollection;
import de.ids_mannheim.korap.resources.Permissions.Permission;
import de.ids_mannheim.korap.security.ac.PolicyBuilder;
import de.ids_mannheim.korap.security.ac.ResourceFinder;
import de.ids_mannheim.korap.security.ac.SecurityManager;
import de.ids_mannheim.korap.user.User;
import de.ids_mannheim.korap.utils.KoralCollectionQueryBuilder;

/**
 * @author hanl
 * @date 15/01/2016
 */
@Deprecated
public class PolicyLoader implements BootableBeanInterface {

	@Override
	public void load(ContextHolder beans) throws KustvaktException {
		SecurityManager.overrideProviders(beans);
		ResourceFinder.overrideProviders(beans);

		User user = User.UserFactory.toUser(KustvaktConfiguration.KUSTVAKT_USER);
		KustvaktConfiguration config = beans.getConfiguration();
		PolicyBuilder builder = new PolicyBuilder(user);
		BufferedReader br;
		try {
			File f = new File(config.getPolicyConfig());
			br = new BufferedReader(new InputStreamReader(new FileInputStream(f)));
		} catch (FileNotFoundException e) {
			throw new KustvaktException("Policy config file: " + config.getPolicyConfig() + " does not exists!",
					e.getCause(), 101);
		}
		String policy = null;
		String[] policyData = null;
		String type, id, name, description, condition;
		String[] permissions;
		try {
			while ((policy = br.readLine()) != null) {
				if (policy.startsWith("#") || policy.isEmpty()){
					continue;
				}
				
				policyData = policy.split("\t");
				type = policyData[0];
				id = policyData[1];
				name = policyData[2];
				description = policyData[3];
				condition = policyData[4];
				permissions = policyData[5].split(",");
				
				String collectionQuery = null;
				if (policyData.length > 6)
					collectionQuery = policyData[6];

				Permission[] permissionArr = new Permission[permissions.length];
				for (int i = 0; i < permissions.length; i++) {
					if (permissions[i].equals("read")) {
						permissionArr[i] = Permission.READ;
					}
				}
				KustvaktResource resource = createResource(type, id, name, description, collectionQuery);
				if (resource != null) {
					builder.addCondition(condition);
					builder.setResources(resource);
					builder.setPermissions(permissionArr);
					builder.create();
				}
			}
			br.close();
		} catch (IOException e) {
			throw new KustvaktException("Failed creating policies.", e.getCause(), 100);
		}
	}

	private KustvaktResource createResource(String type, String id, String name, String description, String docQuery) {
		
		KustvaktResource resource = null;
		if (type.equals("corpus")) {
			resource = new Corpus(id);
		} else if (type.equals("foundry")) {
			resource = new Foundry(id);
		} else if (type.equals("layer")) {
			resource = new Layer(id);
		} else if (type.equals("virtualcollection")) {
			KoralCollectionQueryBuilder builder;
			resource = new VirtualCollection(id);
			if (docQuery != null && !docQuery.isEmpty()) {
				builder = new KoralCollectionQueryBuilder();
				builder.with(docQuery);
				resource.setFields(builder.toJSON());
			}
		} else {
			return resource;
		}

		if (!name.isEmpty()) {
			resource.setName(name);
		}
		if (!description.isEmpty()) {
			resource.setDescription(description);
		}

		return resource;
	}

	@Override
	public Class<? extends BootableBeanInterface>[] getDependencies() {
		return new Class[] { UserLoader.class };
	}
}

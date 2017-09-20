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
import de.ids_mannheim.korap.resources.KustvaktResource;
import de.ids_mannheim.korap.resources.Permissions.Permission;
import de.ids_mannheim.korap.resources.VirtualCollection;
import de.ids_mannheim.korap.security.ac.PolicyBuilder;
import de.ids_mannheim.korap.security.ac.ResourceFinder;
import de.ids_mannheim.korap.security.ac.SecurityManager;
import de.ids_mannheim.korap.user.User;
import de.ids_mannheim.korap.utils.JsonUtils;
import de.ids_mannheim.korap.utils.KoralCollectionQueryBuilder;

/**
 * @author hanl, margaretha
 * @date 12/01/2016
 * @lastupdate 19/04/2017
 */
@Deprecated
public class CollectionLoader implements BootableBeanInterface {

    @Override
    @Deprecated
    public void load (ContextHolder beans) throws KustvaktException {
        SecurityManager.overrideProviders(beans);
        ResourceFinder.overrideProviders(beans);

        User user = User.UserFactory
                .toUser(KustvaktConfiguration.KUSTVAKT_USER);

        KustvaktConfiguration config = beans.getConfiguration();
        PolicyBuilder builder = new PolicyBuilder(user);
        String result = null;
        BufferedReader br;
        try {
            File f = new File(config.getPolicyConfig());
            br = new BufferedReader(
                    new InputStreamReader(new FileInputStream(f)));
        }
        catch (FileNotFoundException e) {
            throw new KustvaktException("Policy config file: "
                    + config.getPolicyConfig() + " does not exists!",
                    e.getCause(), 101);
        }
        String policy = null, collectionQuery = null;
        String[] policyData = null;
        String type, id, name, description, condition;
        String[] permissions;
        try {
            while ((policy = br.readLine()) != null) {
                if (policy.startsWith("#") || policy.isEmpty())
                    continue;
                policyData = policy.split("\t");
                type = policyData[0];
                id = policyData[1];
                name = policyData[2];
                description = policyData[3];
                condition = policyData[4];
                permissions = policyData[5].split(",");
                if (policyData.length > 6)
                    collectionQuery = policyData[6];

                Permission[] permissionArr = new Permission[permissions.length];
                for (int i = 0; i < permissions.length; i++) {
                    if (permissions[i].equals("read")) {
                        permissionArr[i] = Permission.READ;
                    }
                }

                KustvaktResource resource = createResource(type, id, name,
                        description, collectionQuery);
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
        }
        catch (IOException e) {
            throw new KustvaktException("Failed creating virtual collections.",
                    e.getCause(), 100);
        }
    }


    private KustvaktResource createResource (String type, String id,
            String name, String description, String docQuery) {
        KoralCollectionQueryBuilder builder;
        KustvaktResource resource = null;
        if (type.equals("virtualcollection")) {
            resource = new VirtualCollection(id);
            if (!name.isEmpty()) {
                resource.setName(name);
            }
            if (!description.isEmpty()) {
                resource.setDescription(description);
            }
            if (docQuery != null && !docQuery.isEmpty()) {
                builder = new KoralCollectionQueryBuilder();
                builder.with(docQuery);
                resource.setFields(builder.toJSON());
            }
        }

        return resource;
    }


    @Override
    public Class<? extends BootableBeanInterface>[] getDependencies () {
        return new Class[] { UserLoader.class };
    }
}

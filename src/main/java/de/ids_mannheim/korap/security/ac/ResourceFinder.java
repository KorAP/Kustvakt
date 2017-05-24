package de.ids_mannheim.korap.security.ac;

import de.ids_mannheim.korap.config.ContextHolder;
import de.ids_mannheim.korap.config.BeansFactory;
import de.ids_mannheim.korap.exceptions.EmptyResultException;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.exceptions.StatusCodes;
import de.ids_mannheim.korap.interfaces.db.PolicyHandlerIface;
import de.ids_mannheim.korap.interfaces.db.ResourceOperationIface;
import de.ids_mannheim.korap.resources.KustvaktResource;
import de.ids_mannheim.korap.resources.Permissions;
import de.ids_mannheim.korap.resources.ResourceFactory;
import de.ids_mannheim.korap.security.PermissionsBuffer;
import de.ids_mannheim.korap.security.PolicyCondition;
import de.ids_mannheim.korap.security.SecurityPolicy;
import de.ids_mannheim.korap.config.Attributes;
import de.ids_mannheim.korap.user.User;
import lombok.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Created by hanl on 3/20/14.
 */
//todo: use factory pattern to init resourcefinder
public class ResourceFinder {

    private static final Logger jlog = LoggerFactory
            .getLogger(ResourceFinder.class);
    private static PolicyHandlerIface policydao;
    private static Map<Class, ResourceOperationIface> resourcedaos = new HashMap<>();

    private List<KustvaktResource.Container> containers;
    private User user;


    private ResourceFinder (User user) {
        this();
        this.user = user;
    }


    private ResourceFinder () {
        this.containers = new ArrayList<>();
        overrideProviders(null);
    }


    public static void overrideProviders (ContextHolder beans) {
        if (beans == null)
            beans = BeansFactory.getKustvaktContext();
        if (policydao == null | resourcedaos == null) {
            ResourceFinder.policydao = beans.getPolicyDbProvider();
            for (ResourceOperationIface iface : beans.getResourceProviders())
                resourcedaos.put(iface.type(), iface);
        }
        if (policydao == null | resourcedaos.isEmpty())
            throw new RuntimeException("provider not set!");
    }


    public static <T extends KustvaktResource> Set<T> search (String path,
            boolean asParent, User user, Class<T> clazz,
            Permissions.Permission ... perms) throws KustvaktException {
        ResourceFinder cat = init(path, asParent, user, clazz, perms);
        return cat.getResources();
    }


    private static <T extends KustvaktResource> ResourceFinder init (
            String path, boolean asParent, User user, Class<T> clazz,
            Permissions.Permission ... perms) throws KustvaktException {
        ResourceFinder cat = new ResourceFinder(user);
        PermissionsBuffer buffer = new PermissionsBuffer();
        if (perms.length == 0)
            buffer.addPermissions(Permissions.Permission.READ);
        buffer.addPermissions(perms);
        cat.retrievePolicies(path, buffer.getPbyte(), clazz, asParent);
        return cat;
    }


    //todo: needs to be much faster!
    public static <T extends KustvaktResource> ResourceFinder init (
            @NonNull User user, Class<T> clazz) throws KustvaktException {
        return init(null, true, user, clazz, Permissions.Permission.READ);
    }


    public static <T extends KustvaktResource> Set<T> search (String name,
            boolean asParent, User user, String type) throws KustvaktException {
        return (Set<T>) search(name, asParent, user,
                ResourceFactory.getResourceClass(type),
                Permissions.Permission.READ);
    }


    public static <T extends KustvaktResource> Set<T> searchPublic (
            Class<T> clazz) throws KustvaktException {
        return searchPublicFiltered(clazz);
    }

    public static <T extends KustvaktResource> Set<T> searchPublicFilteredIntId (
            Class<T> clazz, int ... ids) throws KustvaktException {
        
        overrideProviders(null);
        Set<T> sets = new HashSet<>();
       
        List<SecurityPolicy> policies = policydao.getPolicies(
                new PolicyCondition(Attributes.PUBLIC_GROUP), clazz,
                Permissions.Permission.READ.toByte());
        ArrayList<Integer> id_set = new ArrayList<>(ids.length);
        for(int id : ids){
            id_set.add(id);
        }
        
        for (SecurityPolicy policy : policies) {
            jlog.debug("PolicyID retrieved: "+policy.getID()+" "+policy.getTarget());
            if (id_set.isEmpty() || id_set.contains(policy.getID())) {
                @SuppressWarnings("unchecked")
                T r = (T) resourcedaos.get(KustvaktResource.class).findbyId(
                        policy.getID(), User.UserFactory.getDemoUser());
                sets.add(r);
            }
        }
        
        if (sets.isEmpty()){
            throw new KustvaktException(StatusCodes.NO_VALUE_FOUND, 
            		"Cannot found public "+clazz.getSimpleName()+" with ids: "+id_set.toString());
        }
        return sets;
    }
    
    public static <T extends KustvaktResource> Set<T> searchPublicFiltered (
            Class<T> clazz, String ... ids) throws KustvaktException {
        overrideProviders(null);
        Set<T> sets = new HashSet<>();
        List<SecurityPolicy> policies = policydao.getPolicies(
                new PolicyCondition(Attributes.PUBLIC_GROUP), clazz,
                Permissions.Permission.READ.toByte());

        List<String> id_set = Arrays.asList(ids);
        for (SecurityPolicy policy : policies) {
            if (id_set.isEmpty() || id_set.contains(policy.getTarget())) {
                @SuppressWarnings("unchecked")
                T r = (T) resourcedaos.get(KustvaktResource.class).findbyId(
                        policy.getTarget(), User.UserFactory.getDemoUser());
                sets.add(r);
            }
        }

        if (sets.isEmpty()){
            throw new KustvaktException(StatusCodes.NO_VALUE_FOUND, 
                    "Cannot found public "+clazz.getSimpleName()+" with ids: "+id_set.toString());
        }
        return sets;
    }


    // todo: should this be working?
    public static <T extends KustvaktResource> Set<T> search (User user,
            Class<T> clazz) throws KustvaktException {
        Set set = new HashSet();
        if (user != null && !User.UserFactory.isDemo(user.getUsername()))
            set = search(null, true, user, clazz, Permissions.Permission.READ);
        if (set.isEmpty())
            set = searchPublic(clazz);
        return set;
    }


    private void retrievePolicies (String path, Byte b, Class type,
            boolean parent) throws KustvaktException {
        //fixme: throw exception to avoid susequent exceptions due to unknown origin
        if (user == null | type == null)
            return;
        if (parent)
            this.containers = policydao.getDescending(path, user, b, type);
        else
            this.containers = policydao.getAscending(path, user, b, type);
    }


    // fixme: only return field --> extra git repo!
    public <T extends KustvaktResource> Set<T> getResources () {
        return evaluateResources();
    }


    // todo: redo with less memory usage/faster
    private <T extends KustvaktResource> Set<T> evaluateResources () {
        Set<T> resources = new HashSet<>();
        if (this.containers != null) {
            for (KustvaktResource.Container c : this.containers) {
                try {
                    T resource = (T) resourcedaos.get(KustvaktResource.class)
                            .findbyId(c.getPersistentID(), this.user);
                    if (resource != null) {
                        PolicyEvaluator e = PolicyEvaluator.setFlags(user,
                                resource);
                        //                        resource.setManaged(e.getFlag("managed", false));
                        resources.add(resource);
                    }
                }
                catch (KustvaktException e) {
                    // don't handle connection error or no handler registered!
                    jlog.error("Error while retrieving containers '{}' ",
                            this.containers);
                    return Collections.emptySet();
                }
            }
        }
        return resources;
    }


    public Set<String> getIds () {
        Set<String> resources = new HashSet<>();
        for (KustvaktResource.Container c : this.containers)
            resources.add(c.getPersistentID());
        return resources;
    }

}

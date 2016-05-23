package de.ids_mannheim.korap.security.ac;

import de.ids_mannheim.korap.config.ContextHolder;
import de.ids_mannheim.korap.config.BeansFactory;
import de.ids_mannheim.korap.exceptions.EmptyResultException;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.exceptions.NotAuthorizedException;
import de.ids_mannheim.korap.exceptions.StatusCodes;
import de.ids_mannheim.korap.interfaces.EncryptionIface;
import de.ids_mannheim.korap.interfaces.db.PolicyHandlerIface;
import de.ids_mannheim.korap.interfaces.db.ResourceOperationIface;
import de.ids_mannheim.korap.resources.KustvaktResource;
import de.ids_mannheim.korap.resources.Permissions;
import de.ids_mannheim.korap.resources.ResourceFactory;
import de.ids_mannheim.korap.security.Parameter;
import de.ids_mannheim.korap.security.PermissionsBuffer;
import de.ids_mannheim.korap.security.PolicyCondition;
import de.ids_mannheim.korap.security.SecurityPolicy;
import de.ids_mannheim.korap.user.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * should only be used if a resource is uniquely identifiable by
 * either three methods: id, name or path!
 * In any other case, use categorypolicies to retrieve policies of a
 * certain type
 * 
 * @author hanl
 * @date 15/01/2014
 */

// todo: add auditing mechanism to this!
@SuppressWarnings("all")
public class SecurityManager<T extends KustvaktResource> {

    private static final Logger jlog = LoggerFactory
            .getLogger(SecurityManager.class);

    private static PolicyHandlerIface policydao;
    @Deprecated
    private static Map<Class<? extends KustvaktResource>, ResourceOperationIface> handlers;
    private static EncryptionIface crypto;

    private List<SecurityPolicy>[] policies;
    private User user;

    private boolean silent;
    private PolicyEvaluator evaluator;
    private T resource;


    //todo: use simple user id if possible! --> or if not check that user has valid integer id (or use username as fallback instead)
    private SecurityManager (User user) {
        this.policies = new List[1];
        this.policies[0] = new ArrayList<>();
        this.silent = true;
        this.user = user;
        overrideProviders(null);
    }


    public static void overrideProviders (ContextHolder beans) {
        if (beans == null)
            beans = BeansFactory.getKustvaktContext();
        if (policydao == null | crypto == null) {
            SecurityManager.policydao = beans.getPolicyDbProvider();
            SecurityManager.crypto = beans.getEncryption();
            SecurityManager.handlers = new HashMap<>();
            Collection<ResourceOperationIface> providers = beans
                    .getResourceProvider();
            for (ResourceOperationIface op : providers)
                SecurityManager.handlers.put(op.type(), op);
        }
        if (policydao == null && crypto == null)
            throw new RuntimeException("providers not set!");
    }


    @Deprecated
    public static final void setProviders (PolicyHandlerIface policyHandler,
            EncryptionIface crypto, Collection<ResourceOperationIface> ifaces) {
        SecurityManager.policydao = policyHandler;
        SecurityManager.crypto = crypto;
        SecurityManager.handlers = new HashMap<>();
        jlog.info("Registering handlers: {}", Arrays.asList(ifaces));
        //        for (ResourceOperationIface iface : ifaces)
        //            handlers.put(iface.getType(), iface);
    }


    /**
     * only allowed if the resource is uniquely identifiable by the
     * name, if not, use path or id!
     * Shortcut so resource values do not need to be retrieved
     * afterwards!
     * 
     * @param name
     * @param user
     * @param type
     * @return
     * @throws EmptyResultException
     * @throws KustvaktException
     */
    //todo: implement a fall back that throws an exception when the user NULL, but the resource has restrictions!
    public static SecurityManager findbyId (String id, User user, Class type,
            Permissions.Permission ... perms) throws KustvaktException {
        SecurityManager p = new SecurityManager(user);
        p.findPolicies(id, false, perms);
        p.resource = p.findResource(type);
        return p;
    }


    public static SecurityManager findbyId (String id, User user,
            Permissions.Permission ... perms) throws KustvaktException {
        SecurityManager p = new SecurityManager(user);
        p.findPolicies(id, false, perms);
        p.resource = p.findResource(null);
        return p;
    }


    public static SecurityManager findbyId (Integer id, User user,
            Permissions.Permission ... perms) throws KustvaktException {
        SecurityManager p = new SecurityManager(user);
        p.findPolicies(id, false, perms);
        p.resource = p.findResource(null);
        return p;
    }


    public static SecurityManager findbyPath (String path, User user,
            Permissions.Permission ... perms) throws NotAuthorizedException,
            EmptyResultException {
        SecurityManager manager = new SecurityManager(user);
        manager.findPolicies(path, true, perms);
        //fixme: need a match count. if match not unique, exception. also, does parent -child relation match hold up here?
        return manager;
    }


    public static SecurityManager init (String id, User user,
            Permissions.Permission ... perms) throws NotAuthorizedException,
            EmptyResultException {
        SecurityManager p = new SecurityManager(user);
        p.findPolicies(id, false, perms);
        return p;
    }


    /**
     * enables retrieval for read access only!
     * 
     * @return
     * @throws NotAuthorizedException
     */
    public final T getResource () throws NotAuthorizedException {
        if (evaluator.isAllowed(Permissions.Permission.READ)) {
            return this.resource;
        }
        else {
            jlog.error(
                    "Reading the resource '{}' is not allowed for user '{}'",
                    this.resource.getPersistentID(), this.user.getUsername());
            throw new NotAuthorizedException(StatusCodes.PERMISSION_DENIED,
                    evaluator.getResourceID());
        }
    }


    public void updateResource (T resource) throws NotAuthorizedException,
            KustvaktException {
        if (evaluator.isAllowed(Permissions.Permission.WRITE)) {
            ResourceOperationIface iface = handlers.get(resource.getClass());
            if (iface != null)
                iface.updateResource(resource, this.user);
            else
                handlers.get(KustvaktResource.class).updateResource(resource,
                        this.user);
        }
        else {
            jlog.error(
                    "Updating the resource '{}' is not allowed for user '{}'",
                    this.resource.getPersistentID(), this.user.getUsername());
            throw new NotAuthorizedException(StatusCodes.PERMISSION_DENIED,
                    this.evaluator.getResourceID());
        }

    }


    /**
     * @throws NotAuthorizedException
     * @throws KustvaktException
     */
    // todo: delete only works with find, not with init constructor!
    public void deleteResource () throws NotAuthorizedException,
            KustvaktException {
        if (evaluator.isAllowed(Permissions.Permission.DELETE)) {
            ResourceOperationIface iface = handlers.get(this.resource
                    .getClass());
            if (iface != null)
                iface.deleteResource(this.evaluator.getResourceID(), this.user);
            else
                handlers.get(KustvaktResource.class).deleteResource(
                        this.evaluator.getResourceID(), this.user);
            this.policydao.deleteResourcePolicies(
                    this.evaluator.getResourceID(), this.user);
        }
        else
            throw new NotAuthorizedException(StatusCodes.PERMISSION_DENIED,
                    this.evaluator.getResourceID());
    }


    // todo: type should be deprecated and return type of policies should be containers!
    private boolean findPolicies (Object id, boolean path,
            Permissions.Permission ... perms) throws EmptyResultException {
        PermissionsBuffer b = new PermissionsBuffer();
        if (perms.length == 0)
            b.addPermission(Permissions.Permission.READ.toByte());
        else
            b.addPermissions(perms);
        if (id instanceof String && !path)
            this.policies = policydao.getPolicies((String) id, this.user,
                    b.getPbyte());
        if (id instanceof String && path)
            this.policies = policydao.findPolicies((String) id, this.user,
                    b.getPbyte());
        if (id instanceof Integer)
            this.policies = policydao.getPolicies((Integer) id, this.user,
                    b.getPbyte());
        this.evaluator = new PolicyEvaluator(this.user, this.policies);

        if (this.policies == null) {
            jlog.error("No policies found for resource id '{}' for user '{}'",
                    id, user.getId());
            throw new EmptyResultException(String.valueOf(id));
        }
        return true;
    }


    // todo:  security log shows id 'null' --> better way?
    private T findResource (Class type) throws NotAuthorizedException,
            KustvaktException {
        if (!evaluator.isAllowed()) {
            jlog.error("Permission denied for resource id '{}' for user '{}'",
                    this.evaluator.getResourceID(), user.getId());
            throw new NotAuthorizedException(StatusCodes.PERMISSION_DENIED,
                    this.evaluator.getResourceID());
        }

        ResourceOperationIface iface = handlers.get(type);
        if (iface == null)
            iface = handlers.get(KustvaktResource.class);
        T resource = (T) iface.findbyId(this.evaluator.getResourceID(),
                this.user);
        // fixme: this
        // fixme: deprecated!
        resource.setManaged(this.evaluator.isManaged());
        resource.setShared(this.evaluator.isShared());
        return resource;
    }


    private boolean checkResource (String persistentID, User user)
            throws KustvaktException {
        ResourceOperationIface iface = handlers.get(KustvaktResource.class);
        return iface.findbyId(persistentID, user) != null;
    }


    public static SecurityManager register (KustvaktResource resource, User user)
            throws KustvaktException, NotAuthorizedException {
        SecurityManager p = new SecurityManager(user);
        if (!User.UserFactory.isDemo(user.getUsername())) {
            if (resource.getParentID() != null) {
                try {
                    // the owner has all rights per default, in order to be able derivate from a parent resource, he needs all permissions as well
                    // this is mostly for convenvience and database consistency, since a request query would result in not authorized, based on missing parent relation dependencies
                    // --> in order not to have a resource owner that is denied access due to missing parent relation dependency
                    SecurityManager.findbyId(resource.getParentID(), user,
                            Permissions.Permission.ALL);
                }
                catch (EmptyResultException e) {
                    jlog.error(
                            "No policies found for parent '{}' for user '{}'",
                            resource.getParentID(), user.getId());
                    throw new KustvaktException(StatusCodes.EMPTY_RESULTS);
                }
            }
            boolean newid = false;
            // create persistent identifier for the resource
            if (resource.getPersistentID() == null
                    || resource.getPersistentID().isEmpty()) {
                ResourceFactory.createID(resource);
                newid = true;
            }

            if (newid | !p.checkResource(resource.getPersistentID(), user)) {
                //                resource.setOwner(user.getId());

                jlog.info("Creating Access Control structure for resource '"
                        + resource.getPersistentID() + "@" + resource.getId()
                        + "', name: " + resource.getName());
                // storing resource is called twice. first when this is register and later in idsbootstrap to create cstorage entry. how to unify this?
                ResourceOperationIface iface = p.handlers.get(resource
                        .getClass());
                if (iface != null)
                    resource.setId(iface.storeResource(resource, user));
                else
                    // retrieve default handler for resource!
                    resource.setId(p.handlers.get(KustvaktResource.class)
                            .storeResource(resource, user));
            }
            p.resource = resource;
            try {
                // todo: which is better? Integer id or String persistentID?
                p.findPolicies(resource.getPersistentID(), false,
                        Permissions.Permission.CREATE_POLICY,
                        Permissions.Permission.READ_POLICY,
                        Permissions.Permission.MODIFY_POLICY);
            }
            catch (EmptyResultException e) {
                jlog.error(
                        "No policies found for '{}' for user '{}'. Resource could not be registered!",
                        resource.getPersistentID(), user.getId());
                throw new KustvaktException(user.getId(),
                        StatusCodes.POLICY_CREATE_ERROR,
                        "Resource could not be registered", resource.toString());
            }
        }
        return p;
    }


    @Deprecated
    public List<SecurityPolicy> getPoliciesList (int i) {
        if (i < this.policies.length)
            return this.policies[i];
        return Collections.emptyList();
    }


    // fixme: make protected
    public SecurityPolicy getPolicy (Integer id) {
        for (SecurityPolicy p : this.policies[0])
            if (p.getID() == id)
                return p;
        return null;
    }


    // fixme: make protected
    public PolicyCondition getExtensional (Permissions.Permission ... pps) {
        for (SecurityPolicy p : this.policies[0]) {
            if (p.equalsPermission(pps)) {
                for (PolicyCondition c : p.getConditions()) {
                    if (c.isExtensional())
                        return c;
                }
            }
        }
        return null;
    }


    private boolean matchTarget (String target) {
        return this.resource.getPersistentID() != null
                && (this.resource.getPersistentID() == target);
    }


    public void addPolicy (SecurityPolicy policy, Parameter ... params)
            throws KustvaktException, NotAuthorizedException {
        if (policy.getConditions().isEmpty()) {
            jlog.error("No conditions set for '{}' for user '{}'",
                    policy.toString(), this.user.getId());
            throw new NotAuthorizedException(StatusCodes.ILLEGAL_ARGUMENT,
                    policy.getTarget());
        }

        if (this.policies[0] == null) {
            jlog.error("No policies found for '{}' for user '{}'",
                    this.evaluator.getResourceID(), this.user.getId());
            throw new NotAuthorizedException(StatusCodes.UNSUPPORTED_OPERATION,
                    policy.getTarget());
        }

        if (contains(policy)) {
            modifyPolicy(policy);
            return;
        }

        if (evaluator.isAllowed(Permissions.Permission.CREATE_POLICY)) {
            policydao.createPolicy(policy, this.user);
        }
        else if (silent) {
            jlog.error(
                    "Permission Denied (CREATE_POLICY) on '{}' for user '{}'",
                    this.evaluator.getResourceID(), this.user.getId());
            throw new NotAuthorizedException(StatusCodes.PERMISSION_DENIED,
                    policy.getTarget());
        }

        if (params != null && params.length > 0) {
            for (Parameter p : params) {
                p.setPolicy(policy);
                policydao.createParamBinding(p);
            }
        }
        this.policies[0].add(policy);
        try {
            Thread.sleep(5);
        }
        catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    public void deletePolicies () throws NotAuthorizedException,
            KustvaktException {
        for (SecurityPolicy p : new ArrayList<>(this.policies[0]))
            deletePolicy(p);
    }


    public void retainPolicies (List<SecurityPolicy> policies)
            throws NotAuthorizedException, KustvaktException {
        for (SecurityPolicy p : new ArrayList<>(this.policies[0])) {
            if (!policies.contains(p))
                this.deletePolicy(p);
        }
    }


    // todo:
    public void deletePolicy (SecurityPolicy policy) throws KustvaktException,
            NotAuthorizedException {
        // todo: get rid of this: use sql to match policy id and target according to evaluator!
        if (!matchTarget(policy.getTarget()))
            // adjust message
            throw new NotAuthorizedException(StatusCodes.ILLEGAL_ARGUMENT,
                    this.evaluator.getResourceID());

        if (this.policies[0] == null) {
            jlog.error("No policies found (DELETE_POLICY) on '{}' for '{}'",
                    this.evaluator.getResourceID(), this.user.getId());
            throw new KustvaktException(user.getId(), StatusCodes.NO_POLICIES,
                    "no policy desicion possible",
                    this.evaluator.getResourceID());
        }
        if (contains(policy)
                && (evaluator.isAllowed(Permissions.Permission.DELETE_POLICY))) {
            policydao.deletePolicy(policy, this.user);
        }
        else if (silent) {
            jlog.error("Permission Denied (DELETE_POLICY) on '{}' for '{}'",
                    this.evaluator.getResourceID(), this.user.getId());
            throw new NotAuthorizedException(StatusCodes.PERMISSION_DENIED,
                    "no policy desicion possible",
                    this.evaluator.getResourceID());
        }
        policydao.removeParamBinding(policy);

        this.policies[0].remove(policy);
    }


    public void modifyPolicy (SecurityPolicy policy) throws KustvaktException,
            NotAuthorizedException {
        if (!matchTarget(policy.getTarget()))
            throw new NotAuthorizedException(StatusCodes.ILLEGAL_ARGUMENT);

        if (this.policies[0] == null) {
            jlog.error(
                    "Operation not possible (MODIFY_POLICY) on '{}' for '{}'",
                    this.evaluator.getResourceID(), this.user.getId());
            throw new KustvaktException(user.getId(), StatusCodes.NO_POLICIES,
                    "no policy desicion possible",
                    this.evaluator.getResourceID());
        }

        if (contains(policy)
                && (evaluator.isAllowed(Permissions.Permission.MODIFY_POLICY))) {
            policydao.updatePolicy(policy, this.user);
        }
        else if (silent) {
            jlog.error("Permission Denied (DELETE_POLICY) on '{}' for '{}'",
                    this.evaluator.getResourceID(), this.user.getId());
            throw new NotAuthorizedException(StatusCodes.PERMISSION_DENIED,
                    this.evaluator.getResourceID());
        }
        this.policies = policydao.getPolicies((int) this.resource.getId(),
                this.user, null);
    }


    /**
     * standard function for READ access on the resource
     * 
     * @return boolean is action allowed for resource
     */
    public boolean isAllowed () {
        return evaluator.isAllowed();
    }


    public boolean isAllowed (Permissions.Permission ... perm) {
        return evaluator.isAllowed();
    }


    /**
     * checks if that exact object already exists (compares name,
     * conditional parameter)
     * 
     * @param policy
     * @return
     */
    public boolean contains (SecurityPolicy policy) {
        try {
            return policydao.checkPolicy(policy, this.user) == 1;
        }
        catch (KustvaktException e) {
            return false;
        }
    }
}

package de.ids_mannheim.korap.security.ac;

import de.ids_mannheim.korap.config.KustvaktCacheable;
import de.ids_mannheim.korap.exceptions.EmptyResultException;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.exceptions.NotAuthorizedException;
import de.ids_mannheim.korap.exceptions.StatusCodes;
import de.ids_mannheim.korap.resources.KustvaktResource;
import de.ids_mannheim.korap.resources.Permissions;
import de.ids_mannheim.korap.resources.ResourceFactory;
import de.ids_mannheim.korap.user.User;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;

/**
 * @author hanl
 * @date 23/03/2014
 */

//todo: use interface (maybe a cachable interface?) and bean instanceing
// todo: if cachable, data integrity needs to be checked! either remove caching or check integrity!
@SuppressWarnings("all")
public class ResourceHandler extends KustvaktCacheable {

    private static Logger jlog = LoggerFactory.getLogger(ResourceHandler.class);


    public ResourceHandler () {
        super("resources", "key:resources");
    }


    @Deprecated
    public <T extends KustvaktResource> T getCache (Object id, Class<T> cz) {
        Element e = CacheManager.getInstance().getCache("resources").get(id);
        if (e != null)
            return (T) e.getObjectValue();
        else
            return null;
    }


    @Deprecated
    public <R extends KustvaktResource> void cache (R resource) {
        CacheManager.getInstance().getCache("resources")
                .put(new Element(resource.getPersistentID(), resource));
    }


    /**
     * @param id
     * @param user
     * @return
     * @throws KustvaktException
     *             if there is no handler registered, resource might
     *             still be valid,
     *             only Notauthorized exception will cause a parsing
     *             error here
     * @throws NotAuthorizedException
     */
    public <T extends KustvaktResource> T findbyIntId (Integer id, User user)
            throws KustvaktException, NotAuthorizedException {
        SecurityManager<T> p;
        try {
            p = SecurityManager.findbyId(id, user);
        }
        catch (EmptyResultException e) {
            throw new NotAuthorizedException(StatusCodes.EMPTY_RESULTS,
                    String.valueOf(id));
        }
        return p.getResource();
    }


    public <T extends KustvaktResource> T findbyStrId (String persistent_id,
            User user, String type) throws KustvaktException,
            NotAuthorizedException {
        return (T) findbyStrId(persistent_id, user,
                ResourceFactory.getResourceClass(type));
    }


    public <T extends KustvaktResource> T findbyStrId (String persistent_id,
            User user, Class<T> type) throws KustvaktException,
            NotAuthorizedException {
        //T cache = (T) getCache(persistent_id, type);
        //if (cache != null)
        //    return cache;
        //else {
        SecurityManager<T> p;
        try {
            p = SecurityManager.findbyId(persistent_id, user, type);
        }
        catch (EmptyResultException e) {
            throw new NotAuthorizedException(StatusCodes.EMPTY_RESULTS,
                    persistent_id);
        }
        return p.getResource();
        //}
    }


    public <T extends KustvaktResource> Collection<T> findbyPath (String path,
            Class type, User user) throws KustvaktException,
            NotAuthorizedException {
        return ResourceFinder.search(path, false, user, type);
    }


    public <T extends KustvaktResource> void updateResources (User user,
            T ... resources) throws KustvaktException, NotAuthorizedException {
        // fixme: what if update fails? then i have a root policy lingering for a resource that is not available?!
        // fixme: transaction management

        for (T resource : resources) {
            SecurityManager policies;
            try {
                policies = SecurityManager.init(resource.getPersistentID(),
                        user, Permissions.Permission.WRITE);
            }
            catch (EmptyResultException e) {
                return;
            }
            policies.updateResource(resource);
        }
    }


    public <T extends KustvaktResource> void storeResources (User user,
            T ... resources) throws KustvaktException, NotAuthorizedException {
        for (T resource : resources)
            SecurityManager.register(resource, user);
    }


    @Deprecated
    public <T extends KustvaktResource> void deleteResources (User user,
            String ... ids) throws KustvaktException, NotAuthorizedException {
        for (String id : ids) {
            SecurityManager policies;
            try {
                policies = SecurityManager.init(id, user,
                        Permissions.Permission.DELETE);
            }
            catch (EmptyResultException e) {
                return;
            }
            policies.deleteResource();
        }
    }


    public <T extends KustvaktResource> void deleteResources (User user,
            T ... resources) throws KustvaktException, NotAuthorizedException {
        for (T r : resources) {
            SecurityManager manager;
            try {
                manager = SecurityManager.findbyId(r.getPersistentID(), user,
                        r.getClass(), Permissions.Permission.DELETE);
            }
            catch (EmptyResultException e) {
                return;
            }
            manager.deleteResource();
        }
    }


    @Deprecated
    public <T extends KustvaktResource> void deleteResources (User user,
            Integer ... ids) throws KustvaktException, NotAuthorizedException {
        for (Integer id : ids) {
            SecurityManager policies;
            try {
                policies = SecurityManager.findbyId(id, user,
                        Permissions.Permission.DELETE);
            }
            catch (EmptyResultException e) {
                return;
            }
            policies.deleteResource();
        }
    }
}

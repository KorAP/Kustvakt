package de.ids_mannheim.korap.security.ac;

import de.ids_mannheim.korap.exceptions.EmptyResultException;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.exceptions.NotAuthorizedException;
import de.ids_mannheim.korap.exceptions.StatusCodes;
import de.ids_mannheim.korap.resources.KustvaktResource;
import de.ids_mannheim.korap.resources.Permissions;
import de.ids_mannheim.korap.resources.ResourceFactory;
import de.ids_mannheim.korap.user.User;
import de.ids_mannheim.korap.utils.KustvaktLogger;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import org.slf4j.Logger;

import java.util.Collection;

/**
 * @author hanl
 * @date 23/03/2014
 */

@SuppressWarnings("all")
public class ResourceHandler {

    private static Logger log = KustvaktLogger.getLogger(ResourceHandler.class);

    public ResourceHandler() {
    }

    public <T extends KustvaktResource> T getCache(Object id, Class<T> clazz) {
        Element e = CacheManager.getInstance().getCache("resources")
                .get(id);
        if (e != null)
            return (T) e.getObjectValue();
        else
            return null;
    }

    public <R extends KustvaktResource> void cache(R resource) {
        CacheManager.getInstance().getCache("resources")
                .put(new Element(resource.getPersistentID(), resource));
    }

    /**
     * @param id
     * @param user
     * @return
     * @throws KustvaktException         if there is no handler registered, resource might still be valid,
     *                                only Notauthorized exception will cause a parsing error here
     * @throws NotAuthorizedException
     */
    public <T extends KustvaktResource> T findbyIntId(Integer id, User user)
            throws KustvaktException, NotAuthorizedException {
        SecurityManager<T> p;
        try {
            p = SecurityManager.findbyId(id, user);
        } catch (EmptyResultException e) {
            throw new NotAuthorizedException(StatusCodes.PERMISSION_DENIED);
        }
        return p.getResource();
    }

    public <T extends KustvaktResource> T findbyStrId(String persistent_id,
            User user, String type)
            throws KustvaktException, NotAuthorizedException {
        T cache = (T) getCache(persistent_id, ResourceFactory
                .getResourceClass(type));
        if (cache != null)
            return cache;
        else
            return (T) findbyStrId(persistent_id, user,
                    ResourceFactory.getResourceClass(type));
    }

    public <T extends KustvaktResource> T findbyStrId(String persistent_id,
            User user, Class<T> type)
            throws KustvaktException, NotAuthorizedException {
        T cache = (T) getCache(persistent_id, type);
        if (cache != null)
            return cache;
        else {
            SecurityManager<T> p;
            try {
                p = SecurityManager.findbyId(persistent_id, user, type);
            } catch (EmptyResultException e) {
                throw new NotAuthorizedException(StatusCodes.EMPTY_RESULTS, persistent_id);
            }
            return p.getResource();
        }
    }

    public <T extends KustvaktResource> Collection<T> findbyPath(String path, Class type, User user)
            throws KustvaktException, NotAuthorizedException {
        return ResourceFinder.search(path, false, user, type);
    }


    public <T extends KustvaktResource> void updateResources(User user, T... resources)
            throws KustvaktException, NotAuthorizedException {
        // fixme: what if update fails? then i have a root policy lingering for a resource that is not available?!
        // fixme: transaction management

        for (T resource : resources) {
            SecurityManager policies;
            try {
                policies = SecurityManager.init(resource.getPersistentID(), user, Permissions.PERMISSIONS.WRITE);
            } catch (EmptyResultException e) {
                return;
            }
            policies.updateResource(resource);
        }
    }

    public <T extends KustvaktResource> void storeResources(User user, T... resources)
            throws KustvaktException, NotAuthorizedException {
        for (T resource : resources)
            SecurityManager.register(resource, user);
    }

    @Deprecated
    public <T extends KustvaktResource> void deleteResources(User user, String... ids)
            throws KustvaktException, NotAuthorizedException {
        for (String id : ids) {
            SecurityManager policies;
            try {
                policies = SecurityManager.init(id, user,
                        Permissions.PERMISSIONS.DELETE);
            } catch (EmptyResultException e) {
                return;
            }
            policies.deleteResource();
        }
    }

    public <T extends KustvaktResource> void deleteResources(User user, T... resources)
            throws KustvaktException, NotAuthorizedException {
        for (T r : resources) {
            SecurityManager policies;
            try {
                policies = SecurityManager.findbyId(r.getPersistentID(), user, r.getClass(),
                        Permissions.PERMISSIONS.DELETE);
            } catch (EmptyResultException e) {
                return;
            }
            policies.deleteResource();
        }
    }

    @Deprecated
    public <T extends KustvaktResource> void deleteResources(User user, Integer... ids)
            throws KustvaktException, NotAuthorizedException {
        for (Integer id : ids) {
            SecurityManager policies;
            try {
                policies = SecurityManager.findbyId(id, user,
                        Permissions.PERMISSIONS.DELETE);
            } catch (EmptyResultException e) {
                return;
            }
            policies.deleteResource();
        }
    }
}

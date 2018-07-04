package de.ids_mannheim.korap.cache;

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

import java.util.Collection;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * @author hanl
 * @date 23/03/2014
 * 
 * @author margaretha
 * @date 01/03/2018
 * 
 * EM: removed resource related code, keep cache
 */

//todo: use interface (maybe a cachable interface?) and bean instanceing
// todo: if cachable, data integrity needs to be checked! either remove caching or check integrity!
@SuppressWarnings("all")
public class ResourceCache extends KustvaktCacheable {

    private static Logger jlog = LogManager.getLogger(ResourceCache.class);

    public ResourceCache () {
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
}

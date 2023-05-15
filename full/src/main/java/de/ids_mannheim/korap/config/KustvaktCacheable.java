package de.ids_mannheim.korap.config;

import java.io.InputStream;
import java.util.Map;

import de.ids_mannheim.korap.utils.ServiceInfo;
import de.ids_mannheim.korap.utils.StringUtils;
import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.PersistenceConfiguration;
import net.sf.ehcache.store.MemoryStoreEvictionPolicy;

/**
 * @author hanl
 * @date 03/02/2016
 */
public abstract class KustvaktCacheable {

    private static boolean loaded = false;
    private String prefix;
    private String name;

    public KustvaktCacheable(String cache_name, String prefix) {
        init();
        if(!enabled())
            createDefaultFileCache(cache_name);
        this.prefix = prefix;
        this.name = cache_name;
    }
    
    public KustvaktCacheable () {
        // TODO Auto-generated constructor stub
    }

    private static Cache getCache(String name) {
        return CacheManager.getInstance().getCache(name);
    }


    private void createDefaultFileCache(String name) {
        Cache default_cache = new Cache(
                new CacheConfiguration(name, 20000)
                        .memoryStoreEvictionPolicy(MemoryStoreEvictionPolicy.LFU)
                        .eternal(false)
                        .timeToLiveSeconds(15000)
                        .timeToIdleSeconds(5000)
                        .diskExpiryThreadIntervalSeconds(0)
                        .persistence(new PersistenceConfiguration().strategy(PersistenceConfiguration.Strategy.LOCALTEMPSWAP)));
        if (!CacheManager.getInstance().cacheExists(name))
            CacheManager.getInstance().addCache(default_cache);
    }


    public void init () {
        if (!loaded) {
            if (ServiceInfo.getInfo().getCacheable()) {
                String file = "ehcache.xml";
                InputStream in = ConfigLoader.loadConfigStream(file);
                CacheManager.newInstance(in);
                loaded = true;
            } else {
                CacheManager.create();
            }
        }
    }

    public boolean hasCacheEntry(Object key) {
        return getCache(this.name).isKeyInCache(createKey(key.toString()));
    }


    public boolean enabled() {
        // check that caching is enabled
        return ServiceInfo.getInfo().getCacheable();
    }

    public Object getCacheValue(Object key) {
        Element e = getCache(this.name).get(createKey(key.toString()));
        if (e!= null)
            return e.getObjectValue();
        return null;
    }

    public long getCacheCreationTime(Object key) {
        Element e = getCache(this.name).get(createKey(key.toString()));
        if (e!= null)
            return e.getCreationTime();
        return -1;
    }

    public void storeInCache(Object key, Object value) {
        getCache(this.name).put(new Element(createKey(key.toString()), value));
    }

    public void removeCacheEntry(Object key) {
        getCache(this.name).remove(createKey(key.toString()));
    }

    public void clearCache() {
        Cache c = getCache(this.name);
        if (enabled()) {
            c.removeAll();
//            c.clearStatistics();
            
        }
    }

    private String createKey(String input) {
        return StringUtils.toSHAHash(this.prefix+ "@" + input);
    }
    
    public Map<Object, Element> getAllCacheElements () {
        Cache cache = getCache(name);
        return cache.getAll(cache.getKeysWithExpiryCheck());
    }
}

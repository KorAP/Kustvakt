package de.ids_mannheim.korap.utils;

import com.google.common.collect.Lists;
import com.google.common.collect.MapMaker;

import java.util.List;
import java.util.concurrent.ConcurrentMap;

import com.google.common.collect.Sets;

import java.util.Collection;
import java.util.Set;


/**
 * A general purpose Multimap implementation for delayed processing and concurrent insertion/deletes.
 * This code is based on an implementation by Guido Medina!
 *
 * @param <K> A comparable Key
 * @param <V> A comparable Value
 */

/**
 * User: hanl
 * Date: 8/27/13
 * Time: 11:18 AM
 */

public class ConcurrentMultiMap<K extends Comparable, V extends Comparable> {

    private final int initialCapacity;
    private final LockMap<K> locks;
    private final ConcurrentMap<K, List<V>> cache;

    public ConcurrentMultiMap() {
        this(16, 64);
    }

    public ConcurrentMultiMap(final int concurrencyLevel) {
        this(concurrencyLevel, 64);
    }

    public ConcurrentMultiMap(final int concurrencyLevel, final int initialCapacity) {
        this.initialCapacity = initialCapacity;
        cache = new MapMaker().concurrencyLevel(concurrencyLevel).initialCapacity(initialCapacity).makeMap();
        locks = new LockMap<K>(concurrencyLevel, initialCapacity);
    }

    public void put(final K key, final V value) {
        synchronized (locks.getLock(key)) {
            List<V> set = cache.get(key);
            if (set == null) {
                set = Lists.newArrayListWithExpectedSize(initialCapacity);
                cache.put(key, set);
            }
            set.add(value);
        }
    }

    public void putAll(final K key, final Collection<V> values) {
        synchronized (locks.getLock(key)) {
            List<V> set = cache.get(key);
            if (set == null) {
                set = Lists.newArrayListWithExpectedSize(initialCapacity);
                cache.put(key, set);
            }
            set.addAll(values);
        }
    }

    public List<V> remove(final K key) {
        synchronized (locks.getLock(key)) {
            return cache.remove(key);
        }
    }


    public void remove(final K key, final V value) {
        List<V> values = cache.get(key);
        synchronized (locks.getLock(key)) {
            values.remove(value);
        }
    }


    public Set<K> getKeySet() {
        return cache.keySet();
    }

    public int size() {
        return cache.size();
    }

    public boolean containsKey(K key) {
        return cache.containsKey(key);
    }

    public List<V> get(K key) {
        return cache.get(key);
    }


    public class LockMap<K extends Comparable> {
        private final ConcurrentMap<K, Object> locks;

        public LockMap() {
            this(16, 64);
        }

        public LockMap(final int concurrencyLevel) {
            this(concurrencyLevel, 64);
        }

        public LockMap(final int concurrencyLevel, final int initialCapacity) {
            locks = new MapMaker().concurrencyLevel(concurrencyLevel).initialCapacity(initialCapacity).weakValues().makeMap();
        }

        public Object getLock(final K key) {
            final Object object = new Object();
            Object lock = locks.putIfAbsent(key, object);
            return lock == null ? object : lock;
        }

    }


    public String toString() {
        return cache.toString();
    }


}

/*
 * Copyright (c) 2002-2022, City of Paris
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *  1. Redistributions of source code must retain the above copyright notice
 *     and the following disclaimer.
 *
 *  2. Redistributions in binary form must reproduce the above copyright notice
 *     and the following disclaimer in the documentation and/or other materials
 *     provided with the distribution.
 *
 *  3. Neither the name of 'Mairie de Paris' nor 'Lutece' nor the names of its
 *     contributors may be used to endorse or promote products derived from
 *     this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 * License 1.0
 */
package fr.paris.lutece.portal.service.cache;

import fr.paris.lutece.portal.service.util.AppLogService;
import jakarta.enterprise.inject.spi.CDI;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.cache.Cache;
import javax.cache.CacheException;
import javax.cache.CacheManager;
import javax.cache.configuration.CacheEntryListenerConfiguration;
import javax.cache.configuration.Configuration;
import javax.cache.configuration.MutableConfiguration;
import javax.cache.integration.CompletionListener;
import javax.cache.processor.EntryProcessor;
import javax.cache.processor.EntryProcessorResult;

/**
 * Base Lutece implementation of the Cache using JCache (JSR-107) and cacheable service.
 * This class implements the JCache (JSR-107) cache API and provides the service for enabling and disabling caches
 * from an administration interface.
 * 
 * @param <K> the type of keys maintained by the cache
 * @param <V> the type of cached values
 */
public abstract class AbstractCacheableService<K, V> implements Lutece107Cache<K, V> {
    
    protected Cache<K, V> _cache;
    protected boolean _bEnable;
    protected Configuration<K, V> configuration; 

    /**
     * Init & create the cache. Should be called by the class (cache) that extends AbstractCacheableService during its initialization.
     */
    public void initCache() {
        initCache(getName());
    }

    /**
     * Init & create the cache. Should be called by the class (cache) that extends AbstractCacheableService during its initialization.
     * 
     * @param strCacheName The cache name
     */
    public void initCache(String strCacheName) {
        createCache(strCacheName);
    }

    /**
     * Init & create the cache. Should be called by the class (cache) that extends AbstractCacheableService during its initialization.
     * 
     * @param strCacheName The cache name
     * @param k The type of keys maintained by the cache
     * @param v The type of cached values
     */
    protected void initCache(String strCacheName, Class<K> k, Class<V> v) {
        createCache(strCacheName, new MutableConfiguration<K, V>().setTypes(k, v));
    }

    /**
     * Init & create the cache.
     * 
     * @param strCacheName The cache name
     * @param configuration a {@link Configuration} for the {@link Cache}
     */
    protected <C extends Configuration<K, V>> void initCache(String strCacheName, C configuration) {
        createCache(strCacheName, configuration);
    }

    /**
     * Create a cache {@link Cache}.
     * 
     * @param strCacheName The cache name
     * @return A cache object
     */
    protected Cache<K, V> createCache(String strCacheName) {
        Configuration<K, V> config = (this.configuration != null) ? this.configuration : new MutableConfiguration<>();
        return createCache(strCacheName, config);
    }

    /**
     * Create a cache for a given Service.
     *
     * @param strCacheName The Cache/Service name
     * @param configuration a {@link Configuration} for the {@link Cache}
     * @return A cache object
     */
    protected <C extends Configuration<K, V>> Cache<K, V> createCache(String strCacheName, C configuration) {
       
    	ILutece107CacheManager luteceCacheManager = CDI.current().select(ILutece107CacheManager.class).get();
        this.configuration = configuration;
        if ((_cache == null || _cache.isClosed()) && (CacheConfigUtil.getStatusFromDataBase(strCacheName) || _bEnable)) {
            _cache = luteceCacheManager.createCache(strCacheName, configuration);
            _bEnable = true;
        }
        CacheService.registerCacheableService(this);
        return _cache;
    }

    /**
     * Put an object into the cache.
     * This method will be removed in the next version. Use {@link #put(K, V)} instead.
     * 
     * @param strKey The key of the object to put into the cache
     * @param object The object to put into the cache
     */
    @Deprecated
    public void putInCache(K strKey, V object) {
        if (isCacheEnable()) {
            _cache.put(strKey, object);
        }
    }

    /**
     * Gets an object from the cache.
     * This method will be removed in the next version. Use {@link #get(K)} instead.
     * 
     * @param strKey The key of the object to retrieve from the cache
     * @return The object from the cache
     */
    @Deprecated
    public V getFromCache(K strKey) {
        return _cache.get(strKey);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public boolean isCacheEnable() {
        return _bEnable;
    }
    /**
     * {@inheritDoc }
     */
    public int getCacheSize() {
        int cacheSize = 0;
        if(_cache != null && !_cache.isClosed( )) {
	        for (Cache.Entry<K, V> entry : _cache) {
	            if (entry != null) {
	            	cacheSize++;
	            }
	        }
        }
        return cacheSize;          
    }
    /**
     * {@inheritDoc }
     */
    @Override
    public void enableCache(boolean bEnable) {
    	_bEnable = bEnable;
        CacheService.updateCacheStatus(this);
        if (!bEnable && (_cache != null && !_cache.isClosed())) {
            _cache.clear();
            _cache.close();
        }

        if (bEnable && (_cache == null || _cache.isClosed())) {
            this.initCache();
        }
        
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public void resetCache() {
        try {
            if (_cache != null && !_cache.isClosed()) {
                _cache.removeAll();
            }
        } catch (CacheException | IllegalStateException e) {
            AppLogService.error(e.getMessage(), e);
        }
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public List<K> getKeys() {
        List<K> keys = new ArrayList<>();
        if(_cache != null) {
	        for (Cache.Entry<K, V> entry : _cache) {
	            if (entry != null) {
	                keys.add(entry.getKey());
	            }
	        }
        }
        return keys;
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public String getInfos() {
        return CacheConfigUtil.getInfos(this.configuration);
    }

    /**
     * Remove a key from the cache.
     * This method will be removed in the next version. Use {@link #remove(K)} instead.
     *
     * @param strKey The key to remove
     */
    @Deprecated
    public void removeKey(K strKey) {
        getCache().getAndRemove(strKey);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public V get(K key) {
        V v = null;
        if ( _cache != null && !_cache.isClosed( ))
        {
            return _cache.get(key);
        }
        return v;
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public Map<K, V> getAll(Set<? extends K> keys) {
        return _cache.getAll(keys);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public boolean containsKey(K key) {
        return _cache.containsKey(key);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public void loadAll(Set<? extends K> keys, boolean replaceExistingValues, CompletionListener completionListener) {
        _cache.loadAll(keys, replaceExistingValues, completionListener);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public void put(K key, V value) {
        if ( _cache != null && !_cache.isClosed( ))
        {
            _cache.put(key, value);
        }
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public V getAndPut(K key, V value) {
        return _cache.getAndPut(key, value);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public void putAll(Map<? extends K, ? extends V> map) {
        _cache.putAll(map);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public boolean putIfAbsent(K key, V value) {
        return _cache.putIfAbsent(key, value);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public boolean remove(K key) {
        return _cache.remove(key);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public boolean remove(K key, V oldValue) {
        return _cache.remove(key, oldValue);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public V getAndRemove(K key) {
        return _cache.getAndRemove(key);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public boolean replace(K key, V oldValue, V newValue) {
        return _cache.replace(key, oldValue, newValue);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public boolean replace(K key, V value) {
        return _cache.replace(key, value);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public V getAndReplace(K key, V value) {
        return _cache.getAndReplace(key, value);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public void removeAll(Set<? extends K> keys) {
        _cache.removeAll(keys);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public void removeAll() {
        _cache.removeAll();
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public void clear() {
        if (_cache != null && !_cache.isClosed()) {
            _cache.clear();
        }
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public <C extends Configuration<K, V>> C getConfiguration(Class<C> clazz) {
        return _cache.getConfiguration(clazz);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public <T> T invoke(K key, EntryProcessor<K, V, T> entryProcessor, Object... arguments) {
        return _cache.invoke(key, entryProcessor, arguments);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public <T> Map<K, EntryProcessorResult<T>> invokeAll(Set<? extends K> keys, EntryProcessor<K, V, T> entryProcessor, Object... arguments) {
        return _cache.invokeAll(keys, entryProcessor, arguments);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public CacheManager getCacheManager() {
        return _cache.getCacheManager();
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public void close() {
        _cache.close();
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public boolean isClosed() {
        return _cache.isClosed();
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public <T> T unwrap(Class<T> clazz) {
        return _cache.unwrap(clazz);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public void registerCacheEntryListener(CacheEntryListenerConfiguration<K, V> cacheEntryListenerConfiguration) {
        _cache.registerCacheEntryListener(cacheEntryListenerConfiguration);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public void deregisterCacheEntryListener(CacheEntryListenerConfiguration<K, V> cacheEntryListenerConfiguration) {
        _cache.deregisterCacheEntryListener(cacheEntryListenerConfiguration);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public Iterator<Entry<K, V>> iterator() {
        return _cache.iterator();
    }

    /**
     * Set cache.
     * 
     * @param cache the cache to set
     */
    public void setCache(Cache<K, V> cache) {
        _cache = cache;
    }

    /**
     * Return a cache object.
     * 
     * @return cache object
     */
    public Cache<K, V> getCache() {
        return _cache;
    }
}
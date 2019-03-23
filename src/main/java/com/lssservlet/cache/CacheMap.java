package com.lssservlet.cache;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.redisson.api.RMap;

import com.lssservlet.core.DataManager;
import com.lssservlet.datamodel.ADSData;
import com.lssservlet.db.JCPersistence;
import com.lssservlet.main.AppServices;

public class CacheMap<K, V> {
    private RMap<K, V> _internalCacheMap = null;
    private long _expireTime = -1;
    protected static final Logger log = LogManager.getLogger(CacheMap.class);

    public CacheMap(RMap<K, V> map, long expireTime) {
        _internalCacheMap = map;
        _expireTime = expireTime;
    }

    public V get(K k) {
        return _internalCacheMap.get(k);
    }

    public void remove(K k) {
        _internalCacheMap.remove(k);
    }

    public void remove(K k, V v) {
        _internalCacheMap.remove(k);

        if (this == CacheManager.getInstance().getKeyCache() || this == CacheManager.getInstance().getExpiredCache()) {
            if (!AppServices.getInstance().isEnableHttpServices())
                return;
            log.debug("CacheMap write: " + k);
            ADSData data = (ADSData) v;
            JCPersistence.getInstance().update(data);
            DataManager.getInstance().onCacheChanged((String) k);
        }
    }

    public int size() {
        return _internalCacheMap.size();
    }

    public void putSkipStore(K k, V v) {
        _internalCacheMap.fastPut(k, v);
        if (_expireTime > 0) {
            _internalCacheMap.expire(_expireTime, TimeUnit.SECONDS);
        }
        if (this == CacheManager.getInstance().getKeyCache() || this == CacheManager.getInstance().getExpiredCache()) {
            if (!AppServices.getInstance().isEnableHttpServices())
                return;
            if (!DataManager.getInstance().isLoadingDB())
                DataManager.getInstance().onCacheChanged((String) k);
        }
    }

    public void put(K k, V v) {
        _internalCacheMap.fastPut(k, v);
        if (_expireTime > 0) {
            _internalCacheMap.expire(_expireTime, TimeUnit.SECONDS);
        }

        if (this == CacheManager.getInstance().getKeyCache() || this == CacheManager.getInstance().getExpiredCache()) {
            if (!AppServices.getInstance().isEnableHttpServices())
                return;
            log.debug("CacheMap write: " + k);
            ADSData data = (ADSData) v;
            JCPersistence.getInstance().update(data);
            DataManager.getInstance().onCacheChanged((String) k);
        }
    }

    public void clear() {
        _internalCacheMap.clear();
    }

    public boolean containsKey(K key) {
        return _internalCacheMap.containsKey(key);
    }

    public void forEach(BiConsumer<? super K, ? super V> action) {
        _internalCacheMap.forEach(action);
    }

    public void forEach(Consumer<Map.Entry<K, V>> action) {
        Objects.requireNonNull(action);
        for (Map.Entry<K, V> entry : entrySet()) {
            action.accept(entry);
        }
    }

    public Set<K> keySet() {
        return _internalCacheMap.keySet();
    }

    public Set<Map.Entry<K, V>> entrySet() {
        return _internalCacheMap.entrySet();
    }
}

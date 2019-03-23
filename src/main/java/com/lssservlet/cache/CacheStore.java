package com.lssservlet.cache;

import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.redisson.api.map.MapLoader;

import com.lssservlet.core.Config;
import com.lssservlet.datamodel.ADSData;
import com.lssservlet.datamodel.ADSDbKey;
import com.lssservlet.db.JCPersistence;
import com.lssservlet.utils.DBUtil;
import com.lssservlet.utils.DataException;
import com.lssservlet.utils.JsonObject;

public class CacheStore implements MapLoader<java.lang.String, ADSData> {

    protected static final Logger log = LogManager.getLogger(CacheStore.class);

    private DBUtil _dbUtil = null;
    private static CacheStore _shareInstance;

    public static CacheStore getInstance() {
        if (_shareInstance == null) {
            synchronized (CacheStore.class) {
                try {
                    _shareInstance = new CacheStore();
                } catch (Exception e) {
                    log.error("init CacheStore error", e);
                }
            }
        }
        return _shareInstance;
    }

    public CacheStore() {
        _dbUtil = new DBUtil(Config.getInstance().getDataSource(), true);
    }

    public ConcurrentHashMap<ADSDbKey.Type, ConcurrentHashMap<String, ADSData>> loadCache(ADSDbKey.CacheType cacheType) {
        try {
            ConcurrentHashMap<ADSDbKey.Type, ConcurrentHashMap<String, ADSData>> data = new ConcurrentHashMap<ADSDbKey.Type, ConcurrentHashMap<String, ADSData>>();
            List<ADSDbKey.Type> models = JCPersistence.getInstance().getModels(cacheType);
            if (models != null) {
                for (ADSDbKey.Type type : models) {
                    if (type.loadCache()) {
                        List<ADSData> results = JCPersistence.getInstance().loadModel(type);
                        if (results != null && results.size() > 0) {
                            ConcurrentHashMap<String, ADSData> typeMap = new ConcurrentHashMap<String, ADSData>();
                            data.put(type, typeMap);
                            for (ADSData d : results) {
                                typeMap.put(d.getCacheKey(), d);
                            }
                        }
                    }
                }
            }
            return data;
        } catch (Exception e) {
            log.error("Failed to load values from cache store.", e);
        }
        return null;
    }

    @Override
    public ADSData load(String key) {
        ADSData data = JCPersistence.getInstance().find(key);
        if (data == null) {
            log.info("CacheStore load: missing " + key);
        }
        log.info("load info: key:{}", key);
        return data;
    }

    @Override
    public Iterable<String> loadAllKeys() {
        log.error("CacheStore loadAllKeys");
        return null;
    }

    public long insert(String table, JsonObject data, JsonObject dupUpdate) {
        if (_dbUtil != null) {
            try {
                return _dbUtil.insert(table, data, dupUpdate);
            } catch (DataException e) {
                log.warn("insert error", e);
            } catch (SQLException e) {
                log.warn("insert error", e);
            }
        }
        return 0;
    }
}

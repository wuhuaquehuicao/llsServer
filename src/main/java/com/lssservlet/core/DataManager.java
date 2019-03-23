package com.lssservlet.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LogEvent;

import com.lssservlet.api.HandleApplication.LogData;
import com.lssservlet.api.HandleApplication.QueryParams;
import com.lssservlet.api.HandleBase.QueryParams1;
import com.lssservlet.cache.CacheManager;
import com.lssservlet.cache.CacheMap;
import com.lssservlet.cache.CacheStore;
import com.lssservlet.datamodel.ADSData;
import com.lssservlet.datamodel.ADSDbKey;
import com.lssservlet.datamodel.ADSDbKey.CacheType;
import com.lssservlet.datamodel.ADSDbKey.EventType;
import com.lssservlet.datamodel.ADSDbKey.Type;
import com.lssservlet.datamodel.ADSDevice;
import com.lssservlet.datamodel.ADSToken;
import com.lssservlet.db.JCPersistence;
import com.lssservlet.exception.ErrorCode;
import com.lssservlet.main.AppServices;
import com.lssservlet.managers.DeviceManager;
import com.lssservlet.utils.AlphaId;
import com.lssservlet.utils.DataException;
import com.lssservlet.utils.EmailUtil;
import com.lssservlet.utils.Json;
import com.lssservlet.utils.JsonArray;
import com.lssservlet.utils.JsonObject;
import com.lssservlet.utils.LexSortSet;
import com.lssservlet.utils.MapSet;
import com.lssservlet.utils.SortSet;
import com.lssservlet.utils.StringUtil;
import com.lssservlet.utils.TaskManager;
import com.lssservlet.utils.ThreadLocalLock;

public class DataManager {
    protected static final Logger log = LogManager.getLogger(DataManager.class);

    public static volatile DataManager sInstance = null;

    private CacheMap<String, ADSData> cacheMap = null;
    private CacheMap<String, ADSData> expireCacheMap = null;

    private ConcurrentHashMap<String, String> _userNameMap = new ConcurrentHashMap<String, String>();

    private boolean isLoadingDB = false;
    private CacheManager cacheManager = null;
    private AlphaId alphaId = AlphaId.Dictionary36();

    private static class LoadedDataInitSortInfo {
        public ADSDbKey.Type type;
        public ConcurrentHashMap<String, ADSData> data;
    }

    public static DataManager getInstance() {
        if (sInstance == null) {
            synchronized (DataManager.class) {
                if (sInstance == null) {
                    sInstance = new DataManager();
                }
            }
        }
        return sInstance;
    }

    public void stop() {
        if (cacheManager != null) {
            cacheManager.stop();

            cacheManager = null;
        }
    }

    // callback from log4j2,don't call it
    public void onLogEvent(final LogEvent event, String content) {
        // send email
        if (Config.getInstance()._serverType != 3 && event.getLevel().isMoreSpecificThan(Level.ERROR)) {
            TaskManager.runTaskOnThreadPool("logTask", 5, handler -> {
                String html = StringUtil.txtToHtml(content);
                try {
                    EmailUtil.getInstance(null).sendMail("jim@justek.us", null,
                            "LSS API server - " + Config.getInstance().getServerName() + ":"
                                    + Config.getInstance().getServerId() + " Error Log",
                            html);
                } catch (Exception e) {
                    log.warn("email log error", e);
                }
            });
        }
    }

    public void start() throws Exception {
        loadDatabase();
        startBackgroundLoopTask();
    }

    private void loadDatabase() throws Exception {
        cacheManager = CacheManager.getInstance();
        cacheManager.start();

        if (cacheManager != null) {
            cacheMap = cacheManager.getKeyCache();
            expireCacheMap = cacheManager.getExpiredCache();
        }

        final String lockKey = "server_init";
        if (cacheManager.tryLock(lockKey)) {
            isLoadingDB = true;

            List<LoadedDataInitSortInfo> loadedMap = new ArrayList<LoadedDataInitSortInfo>();
            if (cacheMap != null) {
                ConcurrentHashMap<ADSDbKey.Type, ConcurrentHashMap<String, ADSData>> cache = CacheStore.getInstance()
                        .loadCache(CacheType.EKeyMap);
                cache.forEach((k, v) -> {
                    LoadedDataInitSortInfo info = new LoadedDataInitSortInfo();
                    info.type = k;
                    info.data = v;
                    loadedMap.add(info);
                });
            }

            if (expireCacheMap != null) {
                ConcurrentHashMap<ADSDbKey.Type, ConcurrentHashMap<String, ADSData>> expireCache = CacheStore
                        .getInstance().loadCache(CacheType.EExpireMap);
                expireCache.forEach((k, v) -> {
                    LoadedDataInitSortInfo info = new LoadedDataInitSortInfo();
                    info.type = k;
                    info.data = v;
                    loadedMap.add(info);
                });
            }

            Collections.sort(loadedMap, new Comparator<LoadedDataInitSortInfo>() {
                @Override
                public int compare(LoadedDataInitSortInfo o1, LoadedDataInitSortInfo o2) {
                    return o1.type.getSortOrder().compareTo(o2.type.getSortOrder());
                }
            });

            for (LoadedDataInitSortInfo info : loadedMap) {
                info.data.forEach((k, data) -> {
                    // update relationship for object
                    onLoadData(data);
                });
            }
        }

        isLoadingDB = false;
    }

    private void startBackgroundLoopTask() {
        if (AppServices.getInstance().isEnableHttpServices()) {
            TaskManager.runTaskOnThread("backgroundLoopTask", bt -> {
                while (!TaskManager.isStop()) {
                    try {
                        detectExpiredToken();
                        checkDeviceActiveStatus();
                        if (!TaskManager.isStop())
                            Thread.sleep(60000);
                    } catch (Exception e) {
                        log.error("backgroundLoopTask error", e);
                    }
                }
                log.warn("backgroundLoopTask thread exit");
            });
        }
    }

    private void detectExpiredToken() {
        ArrayList<ADSToken> tokens = getDataList(Type.EToken);
        if (tokens != null && tokens.size() > 0) {
            tokens.forEach(token -> {
                if (token.isExpired()) {
                    token.delete(true);
                }
            });
        }
    }

    private void checkDeviceActiveStatus() {
        TaskManager.runTaskOnThread("checkDeviceActiveStatus", bt -> {
            try {
                ArrayList<ADSDevice> devices = DeviceManager.getInstance().getDevices(false);
                if (devices != null && devices.size() > 0) {
                    for (ADSDevice device : devices) {
                        if (dbtime() - device.updated_at > Config.getInstance().getDeviceStatusDuration() + 120000)
                            break;
                        if (dbtime() - device.updated_at > Config.getInstance().getDeviceStatusDuration()
                                && device.active == 1) {
                            device.active = 0;
                            device.update(true);
                        }
                    }
                }
            } catch (Exception e) {

            }
        });

    }

    public boolean isLoadingDB() {
        return isLoadingDB;
    }

    public void onCacheChanged(String cacheKey) {
        if (!isLoadingDB()) {
            try {
                // When only has master server, publish too many message, may lead app lock on latch.await
                // CacheManager.getInstance().publishMessage(CacheManager.RedissonDataChangedClosure.class.getName(),
                // cacheKey);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public CacheMap<String, String> getCacheMap(ADSDbKey.Type type, String id) {
        if (type == null) {
            return null;
        }

        return getCacheMap(type.getValue(), id);
    }

    public CacheMap<String, String> getCacheMap(String type, String id) {
        if (cacheManager == null || type == null || type.length() == 0 || id == null || id.length() == 0) {
            return null;
        }

        String name = type + ":" + id;
        return cacheManager.getOrCreateCache(name);
    }

    public void addToCacheMap(ADSDbKey.Type type, String id, String key, String cacheKey) {
        if (key == null || key.length() == 0 || cacheKey == null || cacheKey.length() == 0) {
            return;
        }

        CacheMap<String, String> cacheMap = getCacheMap(type, id);
        cacheMap.put(key, cacheKey);
    }

    public void removeFromCacheMap(ADSDbKey.Type type, String id, String key) {
        if (key == null || key.length() == 0) {
            return;
        }

        CacheMap<String, String> cacheMap = getCacheMap(type, id);
        cacheMap.remove(key);
    }

    public String getCacheKey(ADSDbKey.Type type, String id, String key) {
        if (key == null || key.length() == 0) {
            return null;
        }

        CacheMap<String, String> cacheMap = getCacheMap(type, id);
        return cacheMap.get(key);
    }

    public <M extends ADSData> ArrayList<M> getMapListObject(HashSet<String> cacheKeys) {
        ArrayList<M> result = new ArrayList<M>();
        if (cacheKeys != null) {
            cacheKeys.forEach(handler -> {
                M data = getCache(handler);
                if (data != null && data.flag == 0)
                    result.add(data);
            });
        }
        return result;
    }

    public <M extends ADSData> ArrayList<M> getMapListObject(MapSet<String> mapList) {
        ArrayList<M> result = new ArrayList<M>();
        if (mapList != null) {
            mapList.forEach(handler -> {
                M data = getCache(handler);
                if (data != null && data.flag == 0)
                    result.add(data);
            });
        }
        return result;
    }

    public <M extends ADSData> ArrayList<M> getMapListObject(LexSortSet set, boolean descending) {
        ArrayList<M> result = new ArrayList<M>();
        if (set != null) {
            Collection<String> ret = set.getCollections();
            if (descending) {
                List<String> results = new ArrayList<>(ret);
                Collections.reverse(results);
                ret = results;
            }

            ret.forEach(it -> {
                int index = it.indexOf(":");
                String cacheKey = it.substring(index + 1);
                M data = getCache(cacheKey);
                if (data != null && data.flag == 0) {
                    result.add(data);
                }
            });
        }
        return result;
    }

    public <M extends ADSData> ArrayList<M> getMapListObject(SortSet mapList) {
        return getMapListObject(mapList, true);
    }

    public <M extends ADSData> ArrayList<M> getMapListObject(SortSet mapList, boolean ascending) {
        ArrayList<M> result = new ArrayList<M>();
        if (mapList != null) {
            if (ascending) {
                mapList.forEachA(handler -> {
                    M data = getCache(handler);
                    if (data != null && data.flag == 0)
                        result.add(data);
                    return false;
                });
            } else {
                mapList.forEachD(handler -> {
                    M data = getCache(handler);
                    if (data != null && data.flag == 0)
                        result.add(data);
                    return false;
                });
            }
        }
        return result;
    }

    public <M extends ADSData> ArrayList<M> getMapListObject(SortSet mapList, long recent) {
        if (recent <= 0) {
            return getMapListObject(mapList);
        }
        ArrayList<M> result = new ArrayList<M>();
        if (mapList != null) {
            mapList.forEachD(handler -> {
                M data = getCache(handler);
                if (data != null && data.flag == 1) {
                    if (time() - data.getCreatedTime() < recent) {
                        result.add(data);
                        return false;
                    }
                    return true;
                }
                return false;
            });
        }
        return result;
    }

    public void addToCacheSet(ADSDbKey.Type type, String cacheKey) {
        if (cacheManager == null || cacheKey == null || cacheKey.length() == 0) {
            return;
        }

        MapSet<String> set = cacheManager.getOrCreateSet(type.getValue());
        set.add(cacheKey);
    }

    public void removeFromCacheSet(ADSDbKey.Type type, String cacheKey) {
        if (cacheManager == null || cacheKey == null || cacheKey.length() == 0) {
            return;
        }

        MapSet<String> set = cacheManager.getOrCreateSet(type.getValue());
        set.remove(cacheKey);
    }

    private MapSet<String> getOrCreateSet(String name) {
        if (cacheManager == null || name == null || name.length() == 0) {
            return null;
        }

        return cacheManager.getOrCreateSet(name);
    }

    public MapSet<String> getOrCreateSet(ADSDbKey.Type type) {
        if (type == null) {
            return null;
        }

        return getOrCreateSet(type.getValue());
    }

    public MapSet<String> getOrCreateSet(String model, Object id, String child) {
        return getOrCreateSet(model + ":" + child + ":" + id);
    }

    public SortSet getOrCreateSortSet(String name) {
        if (cacheManager != null)
            return cacheManager.getOrCreateSortSet(name);
        return null;
    }

    public SortSet getOrCreateSortSet(String model, Object id, String child) {
        return getOrCreateSortSet(model + ":" + child + ":" + id);
    }

    public LexSortSet getOrCreateLexSortSet(String model, Object id, String child) {
        if (model == null || model.length() == 0 || id == null || child == null || child.length() == 0) {
            return null;
        }

        return getOrCreateLexSortSet(model + ":" + child + ":" + id);
    }

    public CacheMap<String, String> getOrCreateCacheMap(String name) {
        return cacheManager.getOrCreateCache(name);
    }

    public LexSortSet getOrCreateLexSortSet(String name) {
        if (cacheManager == null || name == null || name.length() == 0) {
            return null;
        }

        return cacheManager.getOrCreateLexSortSet(name);
    }

    public void addToCacheLexSortSet(String model, String key, String cacheKey) {
        LexSortSet set = getOrCreateLexSortSet(model);
        if (set != null) {
            set.add(key + ":" + cacheKey);
        }
    }

    public void removeFromCacheLexSortSet(String model, String key, String cacheKey) {
        LexSortSet set = getOrCreateLexSortSet(model);
        if (set != null) {
            set.remove(key + ":" + cacheKey);
        }
    }

    public CacheMap<String, ADSData> getCacheMap(String cacheKey) {
        if (cacheKey != null && cacheKey.length() > 0) {
            String[] ks = cacheKey.split(":");
            if (ks != null && ks.length == 2) {
                ADSDbKey.Type type = ADSDbKey.Type.fromString(ks[0]);
                if (type.getCacheType() == CacheType.EExpireMap)
                    return expireCacheMap;
                return cacheMap;
            }
        }
        log.warn("unknown key:" + cacheKey);
        return cacheMap;
    }

    @SuppressWarnings("unchecked")
    public <M extends ADSData> M getCacheIgnoreLock(String cacheKey) {
        if (cacheKey != null) {
            CacheMap<String, ADSData> cacheMap = getCacheMap(cacheKey);
            if (cacheMap != null) {
                ADSData m = cacheMap.get(cacheKey);
                if (m != null) {
                    return (M) m;
                }
            }
        }
        return null;
    }

    public <M extends ADSData> M getCache(String cacheKey, boolean inCache) {
        if (cacheKey != null && cacheKey.length() > 0) {
            if (inCache) {
                CacheMap<String, ADSData> cacheMap = getCacheMap(cacheKey);
                if (cacheMap != null && cacheMap.containsKey(cacheKey)) {
                    return getCache(cacheKey);
                } else {
                    return null;
                }
            }
            return getCache(cacheKey);
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public <M extends ADSData> M getCache(String cacheKey) {
        if (cacheKey != null) {
            ADSData data = ThreadLocalLock.get(cacheKey);
            if (data != null) {
                return (M) data;
            }

            CacheMap<String, ADSData> cacheMap = getCacheMap(cacheKey);
            if (cacheMap != null) {
                ADSData m = cacheMap.get(cacheKey);
                if (m != null) {
                    if (ThreadLocalLock.isLock()) {
                        ThreadLocalLock.put(cacheKey, m);
                    }
                    return (M) m;
                }
            }
        }
        return null;
    }

    public ADSData loadFromDatabase(String key) throws DataException {
        if (key == null || key.length() == 0) {
            throw new DataException(ErrorCode.BAD_REQUEST, "Invalid cachekey");
        }

        ADSData data = JCPersistence.getInstance().find(key);
        if (data != null) {
            onLoadData(data);
        } else {
            data = getCache(key);
            if (data != null) {
                onLoadData(data);
            } else {
                throw new DataException(ErrorCode.CACHEKEY_NOT_FOUND, "Not found cachekey: " + key);
            }
        }
        return data;
    }

    public void onLoadData(ADSData data) {
        if (data != null) {
            if (data.flag == 1) {
                CacheMap<String, ADSData> cacheMap = getCacheMap(data.getCacheKey());
                if (cacheMap != null) {
                    cacheMap.remove(data.getCacheKey());
                }
            } else {
                String reason = null;
                ADSData cache = getCache(data.getCacheKey(), true);
                if (cache != null) {
                    if (cache != data) {
                        JsonObject od = cache.toJsonObject().copy();
                        cache.merge(data);
                        data = cache;
                        reason = od.notEqualsReason(data.toJsonObject());
                        if (reason != null) {
                            log.warn("load data from DB:{}, changed:{}", data.getCacheKey(), reason);
                        }
                    }
                } else {
                    try {
                        reason = Json.encodePretty(data);
                        log.warn("load data from DB:{}, new:{}", data.getCacheKey(), reason);
                    } catch (Exception e) {
                    }
                }

                try {
                    data.update(false);
                    if (reason != null) {
                        CacheMap<String, ADSData> cacheMap = getCacheMap(data.getCacheKey());
                        if (cacheMap != null) {
                            cacheMap.putSkipStore(data.getCacheKey(), data);
                        }
                    }
                } catch (Exception e) {
                    log.error("fail to update: {}", e);
                }
            }
        }
    }

    public JsonArray getNodes() {
        if (cacheManager != null) {
            return cacheManager.getNodes();
        }

        return null;
    }

    public String createEncodeId(String seed) {
        return alphaId.encode(seed);
    }

    public String createEncodeId() {
        return alphaId.getEncodeId(Config.getInstance().getServerId());
    }

    public <M extends ADSData> ArrayList<M> getDataList(ADSDbKey.Type type) {
        MapSet<String> set = getOrCreateSet(type);
        if (set != null) {
            ArrayList<M> dataList = new ArrayList<>();
            set.forEach(cacheKey -> {
                M data = getCache(cacheKey);
                dataList.add(data);
            });

            return dataList;
        }

        return null;
    }

    public <M extends ADSData> ArrayList<M> getSortDataList(ADSDbKey.Type type, boolean ascending) {
        SortSet sortSet = getOrCreateSortSet(type.getValue());
        if (sortSet != null)
            return getMapListObject(sortSet, ascending);

        return null;
    }

    public void removeToken(ADSToken token) {
        if (token != null && !token.getId().equals(Config.getInstance().getDefaultAccessToken())) {
            token.delete(true);
        }
    }

    public void updateToken(ADSToken token) {
        if (token != null && !token.getId().equals(Config.getInstance().getDefaultAccessToken())) {
            token.expired_at = time() + Config.getInstance().getTokenExpiry() * 1000;
            token.update(true);
        }
    }

    public JsonArray getJsonArray(List<String> list) {
        JsonArray result = new JsonArray();
        for (String item : list) {
            result.add(item);
        }
        return result;
    }

    public void printCallStack(String info) {
        String title = "==========[" + info + "]===========";
        log.info(title);
        int count = 0;
        for (StackTraceElement ste : Thread.currentThread().getStackTrace()) {
            if (count > 1) {
                if (!ste.getClassName().startsWith("com.lssservlet"))
                    break;
                log.info(ste);
            }
            count++;
            if (count > 20)
                break;
        }
        String end = "=====================";
        while (end.length() < title.length()) {
            end += "=";
        }

        log.info(end);
    }

    public long time() {
        return System.currentTimeMillis();
    }

    public long dbtime() {
        return System.currentTimeMillis() / 1000 * 1000;
    }

    public List<?> queryFromDatabase(QueryParams params) throws DataException {
        ADSDbKey.Type type = ADSDbKey.Type.fromString(params.type);
        if (type == null) {
            throw new DataException(ErrorCode.BAD_REQUEST, "Invalid query type");
        }

        return JCPersistence.getInstance().query(type, params);
    }

    public List<?> queryFromDatabase(QueryParams1 params) throws DataException {
        ADSDbKey.Type type = ADSDbKey.Type.fromString(params.type);
        if (type == null) {
            throw new DataException(ErrorCode.BAD_REQUEST, "Invalid query type");
        }

        return JCPersistence.getInstance().query(type, params);
    }

    public ConcurrentHashMap<String, String> getUserNameMap() {
        return _userNameMap;
    }

    public void addEvent(EventType eventType, String ownerId, String adminId, String description, String data) {
        try {
            if (eventType == EventType.EDeviceLocationChanged)
                ownerId = (ownerId != null) ? ownerId.toLowerCase() : null;

            JsonObject d = new JsonObject();
            d.put("type", eventType.getValue());
            d.put("owner_id", ownerId);
            if (adminId != null)
                d.put("user_id", adminId);
            if (description != null)
                d.put("description", description);
            if (data != null)
                d.put("data", data);
            d.put("created_at", DataManager.getInstance().dbtime());
            CacheStore.getInstance().insert("t_event", d, null);
            log.warn("ad event:" + eventType.getValue() + ", owner id:" + ownerId + ", " + d.toString());
        } catch (Exception e) {
            log.error("addEvent error", e);
        }
    }

    public void addLog(String deviceId, LogData data) {
        try {
            String did = (deviceId != null) ? deviceId.toLowerCase() : null;
            ADSDevice device = ADSDevice.getDevice(did);
            if (device != null) {
                JsonObject d = new JsonObject();
                d.put("device_id", did);
                if (data != null)
                    d.put("data", data.toString());
                d.put("created_at", DataManager.getInstance().dbtime());
                CacheStore.getInstance().insert("t_client_log", d, null);
            }
        } catch (Exception e) {
            log.error("addEvent error", e);
        }
    }
}

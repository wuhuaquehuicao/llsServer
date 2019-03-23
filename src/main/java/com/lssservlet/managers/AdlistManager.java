package com.lssservlet.managers;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.lssservlet.api.HandleAdlist.AdParams;
import com.lssservlet.api.HandleAdlist.AdlistParams;
import com.lssservlet.api.HandleAdlist.PauseParams;
import com.lssservlet.api.HandleBase.QueryParams1;
import com.lssservlet.cache.CacheManager;
import com.lssservlet.cache.CacheMap;
import com.lssservlet.cache.CacheStore;
import com.lssservlet.core.Config;
import com.lssservlet.core.DataManager;
import com.lssservlet.datamodel.ADSAd;
import com.lssservlet.datamodel.ADSAdlist;
import com.lssservlet.datamodel.ADSAdlistAd;
import com.lssservlet.datamodel.ADSDbKey.AdSType;
import com.lssservlet.datamodel.ADSDbKey.Type;
import com.lssservlet.datamodel.ADSDevice;
import com.lssservlet.datamodel.ADSLocation;
import com.lssservlet.datamodel.ADSLocationAdlist;
import com.lssservlet.db.JCPersistence;
import com.lssservlet.exception.ErrorCode;
import com.lssservlet.utils.AlphaId;
import com.lssservlet.utils.DataException;
import com.lssservlet.utils.JsonObject;
import com.lssservlet.utils.SortSet;

public class AdlistManager extends BaseManager {
    private static volatile AdlistManager sInstance = null;
    protected static final Logger log = LogManager.getLogger(AdlistManager.class);

    private AdlistManager() {
    }

    public static AdlistManager getInstance() {
        if (sInstance == null) {
            synchronized (AdlistManager.class) {
                if (sInstance == null) {
                    sInstance = new AdlistManager();
                }
            }
        }

        return sInstance;
    }

    public ADSAdlist getAdlist(String aid) throws DataException {
        return checkExist(ADSAdlist.getCacheKey(aid), true);
    }

    public ArrayList<ADSAdlist> getAdlists(boolean ascending) {
        return DataManager.getInstance().getSortDataList(Type.EAdlist, ascending);
    }

    // public ADSAdlist getAdlistByDeviceId(String deviceId) throws DataException {
    // ArrayList<ADSAdlist> ads = null;
    // ADSAdlist lated = null;
    //
    // if (deviceId == null || deviceId.isEmpty()) {
    // ads = DataManager.getInstance().getDataList(Type.EAdlist);
    // for (ADSAdlist ad : ads) {
    // if (lated == null) {
    // lated = ad;
    // } else if (ad.compareTo(lated) < 0) {
    // lated = ad;
    // }
    // }
    // } else {
    // ADSDevice device = ADSDevice.getDevice(deviceId);
    // if (device == null)
    // throw new DataException(ErrorCode.DEVICE_NOT_FOUND_DEVICE, "Not found device: " + deviceId);
    // ADSLocation location = ADSLocation.getLocation(device.location_id);
    // if (location == null)
    // throw new DataException(ErrorCode.LOCATION_NOT_FOUND_LOCATION, "Not found location.");
    // ads = location.getAdlists();
    // if (ads != null && ads.size() > 0)
    // lated = ads.get(0);
    // }
    //
    // return lated;
    // }

    public ADSAdlist getActiveAdlistByDeviceId(String deviceId) throws DataException {
        ADSAdlist result = null;
        String did = (deviceId != null) ? deviceId.toLowerCase() : null;

        if (did == null || did.isEmpty()) {
            throw new DataException(ErrorCode.DEVICE_INVALID_MAC, "Invalid mac.");
        } else {
            ADSDevice device = ADSDevice.getDevice(did);
            if (device == null)
                throw new DataException(ErrorCode.DEVICE_NOT_FOUND_DEVICE, "Not found device: " + deviceId);
            ADSLocation location = ADSLocation.getLocation(device.location_id);
            if (location == null)
                throw new DataException(ErrorCode.DEVICE_NO_LOCATION_ALLOCATED,
                        "No location allocated for device: " + deviceId);
            result = location.getActiveAdlist();
        }

        return result;
    }

    public ADSAdlist createAdlist(String name, Integer layout, String description, Long slideInterval, String adminId)
            throws DataException {
        ADSAdlist result = new ADSAdlist();
        result.id = "adl_" + AlphaId.generateID();
        if (name != null)
            result.name = name;
        if (description != null)
            result.description = description;
        if (slideInterval != null)
            result.slide_interval = slideInterval;
        result.flag = 0;
        result.created_at = DataManager.getInstance().dbtime();
        result.updated_at = result.created_at;
        result.layout = layout;
        result.created_by = adminId;
        result.update(true);
        return result;
    }

    public ADSAdlist updateAdlist(String adlistId, String name, Integer layout, String description, Long slideInterval,
            boolean restore) throws DataException {
        if (adlistId == null || adlistId.isEmpty())
            throw new DataException(ErrorCode.ADLIST_INVALID_ADLIST, "Invalid adlist.");

        ADSAdlist result = ADSAdlist.getAdlist(adlistId);
        if (result == null && restore)
            result = (ADSAdlist) DataManager.getInstance().loadFromDatabase(ADSAdlist.getCacheKey(adlistId));

        if (result == null)
            throw new DataException(ErrorCode.ADLIST_NOT_FOUND_ADLIST, "Not found adlist: " + adlistId);

        if (name != null)
            result.name = name;
        if (description != null)
            result.description = description;
        if (layout != null)
            result.layout = layout;
        if (slideInterval != null)
            result.slide_interval = slideInterval;
        if (restore)
            result.flag = 0;
        result.updated_at = DataManager.getInstance().dbtime();
        result.update(true);
        return result;
    }

    public void createAdlistRunningTimeHistory(String adlistId, AdlistParams adlistParams, String deviceId)
            throws DataException {
        ADSAdlist adlist = ADSAdlist.getAdlist(adlistId);
        if (adlist == null || adlistParams == null || adlistParams.ads == null || adlistParams.ads.size() == 0)
            throw new DataException(ErrorCode.BAD_REQUEST, "Invalid ads.");

        String did = (deviceId != null) ? deviceId.toLowerCase() : null;
        if (adlistId.equals(getActiveAdlistByDeviceId(did).getId())) {
            JsonObject data = new JsonObject();
            data.put("adlist id:", adlistId);
            // DataManager.getInstance().addEvent(EventType.EAdlistDownloaded, did, null, data.toString());
        }
        for (AdParams ad : adlistParams.ads) {
            if (ad.running_time > Config.getInstance().getStaticsInterval())
                log.warn("device: {} running time exception: {}, statics interval: {}", deviceId, ad.running_time,
                        Config.getInstance().getStaticsInterval());
            addAdRunningTimeHistory(did, ad.id, ad.running_time);
            if (ad.pauses != null && ad.pauses.size() > 0) {
                for (PauseParams pause : ad.pauses) {
                    addAdPauseHistory(ad.id, did, pause.time, pause.duration);
                }
            }
            // ADSAd adsAd = ADSAd.getAd(ad.id);
            // if (adsAd != null) {
            // if (adsAd.running_time == 0) {
            // adsAd.running_time = getTotalRunningTimeForAd(ad.id);
            // }
            // adsAd.running_time += ad.running_time;
            // if (adsAd.running_time <= 0)
            // adsAd.running_time = 0l;
            // adsAd.update(true);
            // }
        }
    }

    public ADSAdlist addAds(String adlistId, AdlistParams param, String adminId) throws DataException {
        if (param == null || param.ads == null || param.ads.size() == 0)
            throw new DataException(ErrorCode.BAD_REQUEST, "Invalid ad.");

        ADSAdlist adlist = ADSAdlist.getAdlist(adlistId);
        if (adlist == null)
            throw new DataException(ErrorCode.ADLIST_NOT_FOUND_ADLIST, "Not found ad-list: " + adlistId);

        for (AdParams ad : param.ads) {
            ADSAd file = null;
            if (ad.id != null) {
                ADSAd oldAd = ADSAd.getAd(ad.id);
                if (oldAd != null)
                    file = oldAd;
            }
            if (file == null && ad.id == null) {
                file = new ADSAd();
                file.id = "ad_" + AlphaId.generateID();
                // file.adlist_id = adlistId;
                file.name = ad.name;
                if (ad.label != null)
                    file.label = ad.label;
                else
                    file.label = adlist.name + " ad " + (adlist.getAds().size() + 1);
                file.path = ad.path;
                file.media_type = ad.media_type;
                file.created_at = DataManager.getInstance().dbtime();
                file.updated_at = file.created_at;
                file.flag = 0;
                file.update(true);
            }

            if (file != null && adlist.getAdMap().get(file.getId()) == null)
                createAdlistAd(adlistId, file.getId(), adminId);
        }
        adlist.updated_at = DataManager.getInstance().dbtime();
        adlist.update(true);
        return ADSAdlist.getAdlist(adlistId);
    }

    public ADSAd updateAd(String adId, String name, String path, String mediaType) throws DataException {
        if (adId == null || adId.isEmpty())
            throw new DataException(ErrorCode.ADLIST_INVALID_ADLIST, "Invalid ad.");

        ADSAd result = ADSAd.getAd(adId);
        if (result == null)
            throw new DataException(ErrorCode.ADLIST_NOT_FOUND_ADLIST, "Not found ad: " + adId);

        if (name != null)
            result.name = name;
        if (path != null)
            result.path = path;
        if (mediaType != null)
            result.media_type = mediaType;
        result.updated_at = DataManager.getInstance().dbtime();
        result.update(true);
        return result;
    }

    public void deleteAd(String adId) throws DataException {
        if (adId == null || adId.isEmpty())
            throw new DataException(ErrorCode.ADLIST_INVALID_ADLIST, "Invalid ad.");

        ADSAd result = ADSAd.getAd(adId);
        if (result == null)
            throw new DataException(ErrorCode.ADLIST_NOT_FOUND_ADLIST, "Not found ad: " + adId);
        result.delete(true);
    }

    public void deleteAd(String adlistId, String adId) throws DataException {
        if (adlistId == null || adlistId.isEmpty() || adId == null || adId.isEmpty())
            throw new DataException(ErrorCode.ADLIST_NOT_FOUND_AD, "Not found ad: " + adId);

        ADSAd result = ADSAd.getAd(adId);
        if (result == null)
            throw new DataException(ErrorCode.ADLIST_NOT_FOUND_AD, "Not found ad: " + adId);
        ADSAdlist adlist = ADSAdlist.getAdlist(adlistId);
        if (adlist == null)
            throw new DataException(ErrorCode.ADLIST_NOT_FOUND_ADLIST, "Not found ad-list: " + adlistId);

        ADSAdlistAd aa = DataManager.getInstance().getCache(adlist.getAdMap().get(adId));
        if (aa != null)
            aa.delete(true);
        adlist.updated_at = DataManager.getInstance().dbtime();
        adlist.update(true);
    }

    public void deleteAdlist(String adlistId) throws DataException {
        ADSAdlist adlist = ADSAdlist.getAdlist(adlistId);
        if (adlist != null) {
            Set<String> keys = adlist.getLocationMap().keySet();
            for (String key : keys) {
                String laCacheKey = adlist.getLocationMap().get(key);
                ADSLocationAdlist oldla = DataManager.getInstance().getCache(laCacheKey);
                if (oldla != null) {
                    oldla.delete(true);
                } else {
                    log.info("not found local-adlist for location: " + key + ", laCacheKey: " + laCacheKey);
                }
            }
            adlist.delete(true);
        }
    }

    public ArrayList<ADSAd> getAdStatics(Long from, Long to, String order, Integer desc) {
        ArrayList<ADSAd> result = getSortedAdStatics(from, to, order, desc);
        if (result.size() > 0)
            return result;

        // SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
        String fromStr = sdf.format(new Date(from));
        String toStr = sdf.format(new Date(to));
        // TimeZone tz = TimeZone.getTimeZone("Asia/Hong_Kong");
        // sdf.setTimeZone(tz);
        QueryParams1 params = new QueryParams1();
        params.clauses = new ArrayList<>();
        params.clauses.add("flag=0");
        // params.clauses.add("created_at>='" + fromStr + "'");
        // params.clauses.add("created_at<='" + toStr + "'");
        params.clauses.add("yyyymmdd>=" + Integer.parseInt(fromStr));
        params.clauses.add("yyyymmdd<=" + Integer.parseInt(toStr));
        params.orders = new ArrayList<>();
        params.groups = new ArrayList<>();
        params.groups.add("ad_id");
        // StringBuilder sql_statement = new StringBuilder(
        // "SELECT ad_id, SUM(running_time) as running_time, SUM(pause_count) as pause_count, created_at FROM
        // t_ad_runningtime_history WHERE flag='0' AND");
        // sql_statement.append(" created_at>='" + fromStr + "'");
        // sql_statement.append(" AND");
        // sql_statement.append(" created_at<='" + toStr + "'");
        // sql_statement.append(" GROUP BY ad_id");
        // if (order != null) {
        // sql_statement.append(" ORDER BY " + order);
        // if (desc == null || desc == 1) {
        // sql_statement.append(" DESC");
        // }
        // } else {
        // sql_statement.append(" ORDER BY created_at DESC");
        // }
        // sql_statement.append(";");
        CacheMap<String, String> adsCacheMap = CacheManager.getInstance().getOrCreateCache("ads:" + from + ":" + to,
                120l);
        try {
            // List<?> rows = JCPersistence.getInstance().query("t_ad_runningtime_history", params);
            // List<?> rows = JCPersistence.getInstance().query("t_ad_runningtime_history_1", params);
            List<?> rows = JCPersistence.getInstance().query("t_ad_runningtime_history_statistic", params);

            // List<?> rows = JCPersistence.getInstance().query(sql_statement.toString());
            for (Object row : rows) {
                HashMap<String, Object> map = (HashMap<String, Object>) row;
                String adId = (String) map.get("ad_id");
                ADSAd ad = ADSAd.getAd(adId);

                if (ad != null) {
                    ad.running_time = getTotalTimeForAd(adId, fromStr, toStr);
                    // ad.running_time = Long.parseLong(map.get("running_time").toString());
                    ad.total_times = (int) (ad.running_time / ad.getSlideInterval())
                            + ((ad.running_time % ad.getSlideInterval() == 0) ? 0 : 1);
                    ad.total_devices = getTotalDevicesForAd(adId, fromStr, toStr);
                    ad.total_locations = getTotalLocationsForAd(adId, fromStr, toStr);
                    ad.total_pauses = getTotalPausesForAd(adId, fromStr, toStr);
                    // ad.total_pauses = Integer.parseInt(map.get("pause_count").toString());
                    adsCacheMap.put(ad.getCacheKey(), ad.toJsonObject().toString());
                    // result.add(ad);
                }
            }
        } catch (Exception e) {
            // TODO: handle exception
            log.error(e);
        }
        buildSortSets(adsCacheMap, from, to);
        return getSortedAdStatics(from, to, order, desc);
    }

    private Long getTotalTimeForAd(String adId, String fromStr, String toStr) {
        Long result = 0l;
        QueryParams1 params = new QueryParams1();
        params.orders = new ArrayList<>();
        // params.orders.add("created_at");
        params.clauses = new ArrayList<>();
        params.clauses.add("ad_id='" + adId + "'");
        params.clauses.add("flag=0");
        // params.clauses.add("created_at>='" + fromStr + "'");
        // params.clauses.add("created_at<='" + toStr + "'");
        params.clauses.add("yyyymmdd>=" + Integer.valueOf(fromStr));
        params.clauses.add("yyyymmdd<=" + Integer.valueOf(toStr));

        try {
            // List<?> rows = JCPersistence.getInstance().query("t_ad_runningtime_history", params);
            // List<?> rows = JCPersistence.getInstance().query("t_ad_runningtime_history_1", params);
            List<?> rows = JCPersistence.getInstance().query("t_ad_runningtime_history_statistic", params);

            for (Object row : rows) {
                HashMap<String, Object> map = (HashMap<String, Object>) row;
                Long rt = Long.parseLong(map.get("running_time").toString());
                result += rt;
            }
        } catch (Exception e) {
            // TODO: handle exception
        }
        return result;
    }

    private Integer getTotalDevicesForAd(String adId, String fromStr, String toStr) {
        Integer result = 0;
        QueryParams1 params = new QueryParams1();
        params.orders = new ArrayList<>();
        // params.orders.add("created_at");
        params.clauses = new ArrayList<>();
        params.clauses.add("ad_id='" + adId + "'");
        params.clauses.add("flag=0");
        // params.clauses.add("created_at>='" + fromStr + "'");
        // params.clauses.add("created_at<='" + toStr + "'");
        params.clauses.add("yyyymmdd>=" + Integer.valueOf(fromStr));
        params.clauses.add("yyyymmdd<=" + Integer.valueOf(toStr));
        params.groups = new ArrayList<>();
        params.groups.add("device_id");
        try {
            // List<?> rows = JCPersistence.getInstance().query("t_ad_runningtime_history", params);
            // List<?> rows = JCPersistence.getInstance().query("t_ad_runningtime_history_1", params);
            List<?> rows = JCPersistence.getInstance().query("t_ad_runningtime_history_statistic", params);
            result = rows.size();
        } catch (Exception e) {
            // TODO: handle exception
        }
        return result;
    }

    private Integer getTotalLocationsForAd(String adId, String fromStr, String toStr) {
        Integer result = 0;
        QueryParams1 params = new QueryParams1();
        params.orders = new ArrayList<>();
        // params.orders.add("created_at");
        params.clauses = new ArrayList<>();
        params.clauses.add("ad_id='" + adId + "'");
        params.clauses.add("flag=0");
        // params.clauses.add("created_at>='" + fromStr + "'");
        // params.clauses.add("created_at<='" + toStr + "'");
        params.clauses.add("yyyymmdd>=" + Integer.valueOf(fromStr));
        params.clauses.add("yyyymmdd<=" + Integer.valueOf(toStr));
        params.groups = new ArrayList<>();
        params.groups.add("location_id");
        params.or = true;
        try {
            // List<?> rows = JCPersistence.getInstance().query("t_ad_runningtime_history", params);
            // List<?> rows = JCPersistence.getInstance().query("t_ad_runningtime_history_1", params);
            List<?> rows = JCPersistence.getInstance().query("t_ad_runningtime_history_statistic", params);
            result = rows.size();
        } catch (Exception e) {
            // TODO: handle exception
        }
        return result;
    }

    private Integer getTotalPausesForAd(String adId, String fromStr, String toStr) {
        Integer result = 0;
        QueryParams1 params = new QueryParams1();
        params.clauses = new ArrayList<>();
        params.clauses.add("ad_id='" + adId + "'");
        params.clauses.add("flag=0");
        params.clauses.add("created_at>='" + fromStr + "'");
        params.clauses.add("created_at<='" + toStr + "'");
        try {
            List<?> rows = JCPersistence.getInstance().query("t_ad_pause_history", params);
            result = rows.size();
        } catch (Exception e) {
            // TODO: handle exception
        }
        return result;
    }

    public void addAdRunningTimeHistory(String deviceId, String adId, Long runningTime) {
        try {
            ADSAd ad = ADSAd.getAd(adId);
            String did = (deviceId != null) ? deviceId.toLowerCase() : null;
            ADSDevice device = ADSDevice.getDevice(did);
            if (ad != null && device != null) {
                JsonObject d = new JsonObject();
                d.put("ad_id", adId);
                d.put("device_id", did);
                d.put("ad_label", (ad.label != null) ? ad.label : "");
                d.put("running_time", runningTime);
                d.put("location_id", device.location_id);
                d.put("created_at", DataManager.getInstance().dbtime());
                CacheStore.getInstance().insert("t_ad_runningtime_history", d, null);
                {
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
                    String yyyymmdd = sdf.format(new Date(DataManager.getInstance().dbtime()));
                    d.put("yyyymmdd", Integer.valueOf(yyyymmdd));
                    d.remove("created_at");
                    CacheStore.getInstance().insert("t_ad_runningtime_history_1", d, null);
                }
            }
        } catch (Exception e) {
            log.error("addAdRunningTimeHistory error", e);
        }
    }

    public void addAdPauseHistory(String adId, String deviceId, Long paused_at, Integer duration) {
        try {
            ADSAd ad = ADSAd.getAd(adId);
            String did = (deviceId != null) ? deviceId.toLowerCase() : null;
            ADSDevice device = ADSDevice.getDevice(did);
            if (ad != null && device != null) {
                JsonObject d = new JsonObject();
                d.put("ad_id", adId);
                d.put("device_id", did);
                Date pausedAt = new Date(paused_at);
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                d.put("paused_at", sdf.format(pausedAt));
                d.put("duration", (duration != null) ? duration : 0l);
                d.put("created_at", DataManager.getInstance().dbtime());
                CacheStore.getInstance().insert("t_ad_pause_history", d, null);
                log.warn("ad pause history:" + ad.label + " device id:" + did + " " + d.toString());
            }
        } catch (Exception e) {
            log.error("addAdPauseHistory error", e);
        }
    }

    public ADSAdlistAd createAdlistAd(String adlistId, String adId, String adminId) throws DataException {
        if (adId == null || adId.isEmpty() || adlistId == null || adlistId.isEmpty())
            throw new DataException(ErrorCode.ADLIST_INVALID_ADLIST, "Invalid ad or adlist");

        ADSAdlistAd result = new ADSAdlistAd();
        result.id = "aa_" + AlphaId.generateID();
        result.ad_id = adId;
        result.adlist_id = adlistId;
        result.flag = 0;
        result.created_at = DataManager.getInstance().dbtime();
        result.updated_at = result.created_at;
        result.created_by = adminId;
        result.update(true);
        return result;
    }

    private void buildSortSets(CacheMap<String, String> adsCacheMap, Long from, Long to) {
        buildSortSet(adsCacheMap, from, to, AdSType.ECreatedAt);
        buildSortSet(adsCacheMap, from, to, AdSType.ERunningTime);
        buildSortSet(adsCacheMap, from, to, AdSType.ETotalTimes);
        buildSortSet(adsCacheMap, from, to, AdSType.ETotalDevices);
        buildSortSet(adsCacheMap, from, to, AdSType.ETotalLocations);
        buildSortSet(adsCacheMap, from, to, AdSType.ETotalPauses);
    }

    private void buildSortSet(CacheMap<String, String> adsCacheMap, Long from, Long to, AdSType type) {
        SortSet sortSet = DataManager.getInstance()
                .getOrCreateSortSet("ads:" + from + ":" + to + ":" + type.getValue());
        if (sortSet.size() <= 0 && adsCacheMap.size() > 0) {
            adsCacheMap.forEach((k, v) -> {
                ADSAd ad = (ADSAd) ADSAd.fromJson(new JsonObject(v));
                sortSet.add(k, ad.getStatistic(type));
            });
        }
    }

    private ArrayList<ADSAd> getSortedAdStatics(Long from, Long to, String order, Integer desc) {
        AdSType type = AdSType.ECreatedAt;
        SortSet sortSet = null;
        if (order != null && !order.isEmpty()) {
            if (order.equals("running_time")) {
                type = AdSType.ERunningTime;
            } else if (order.equals("total_times")) {
                type = AdSType.ETotalTimes;
            } else if (order.equals("total_devices")) {
                type = AdSType.ETotalDevices;
            } else if (order.equals("total_locations")) {
                type = AdSType.ETotalLocations;
            } else if (order.equals("total_pauses")) {
                type = AdSType.ETotalPauses;
            }
        }
        sortSet = DataManager.getInstance().getOrCreateSortSet("ads:" + from + ":" + to + ":" + type.getValue());
        return getMapList(sortSet, from, to, order, desc);
    }

    private ArrayList<ADSAd> getMapList(SortSet sortSet, Long from, Long to, String order, Integer desc) {
        ArrayList<ADSAd> result = new ArrayList<>();
        CacheMap<String, String> adsCacheMap = CacheManager.getInstance().getOrCreateCache("ads:" + from + ":" + to,
                90l);
        if (adsCacheMap.size() > 0) {
            if (desc != null && desc == 0) {
                sortSet.forEachA(handler -> {
                    String v = adsCacheMap.get(handler);
                    ADSAd ad = (ADSAd) ADSAd.fromJson(new JsonObject(v));
                    if (ad != null && ad.flag == 0)
                        result.add(ad);
                    return false;
                });
            } else {
                sortSet.forEachD(handler -> {
                    String v = adsCacheMap.get(handler);
                    ADSAd ad = (ADSAd) ADSAd.fromJson(new JsonObject(v));
                    if (ad != null && ad.flag == 0)
                        result.add(ad);
                    return false;
                });
            }
        }
        return result;
    }
}

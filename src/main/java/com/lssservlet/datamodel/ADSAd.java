package com.lssservlet.datamodel;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Transient;

import com.lssservlet.cache.CacheMap;
import com.lssservlet.core.DataManager;
import com.lssservlet.datamodel.ADSDbKey.AdSType;
import com.lssservlet.datamodel.ADSDbKey.Type;
import com.lssservlet.utils.JCModel;

@SuppressWarnings("serial")
@Entity
@Access(AccessType.FIELD)
@Table(name = "t_ad")
@JCModel(type = Type.EAd)
public class ADSAd extends ADSBase {
    @Column(columnDefinition = ADSDbKey.Column.VARCHAR64_NOT_NULL)
    public String adlist_id;

    @Column(columnDefinition = ADSDbKey.Column.VARCHAR128_NOT_NULL)
    public String name;

    @Column(columnDefinition = ADSDbKey.Column.VARCHAR128)
    public String label;

    @Column(columnDefinition = ADSDbKey.Column.VARCHAR512)
    public String path;

    @Column(columnDefinition = ADSDbKey.Column.VARCHAR64_NOT_NULL)
    public String media_type; // image, video

    @Transient
    public Long running_time = 0l;

    @Transient
    public Integer total_times = 0;

    @Transient
    public Integer total_devices = 0;

    @Transient
    public Integer total_locations = 0;

    @Transient
    public Integer total_pauses = 0;

    @Column(columnDefinition = ADSDbKey.Column.VARCHAR64)
    public String created_by;
    // public ADSAd() {
    // log.info("****** ad constructor ********");
    // }

    public static String getCacheKey(String adId) {
        return getCacheKey(Type.EAd, adId);
    }

    public static ADSAd getAd(String adId) {
        return getCache(Type.EAd, adId);
    }

    public CacheMap<String, String> getDeviceMap() {
        return DataManager.getInstance().getOrCreateCacheMap(
                ADSDbKey.Type.EAd.getValue() + ":" + ADSDbKey.Type.EDevice.getValue() + ":" + getId());
    }

    public Long getSlideInterval() {
        Long result = 15l;
        ADSAdlist adlist = ADSAdlist.getAdlist(adlist_id);
        if (adlist != null)
            result = adlist.slide_interval;
        return result;
    }

    public Long getStatistic(AdSType type) {
        Long result = null;
        if (type == AdSType.ERunningTime)
            result = running_time;
        else if (type == AdSType.ETotalTimes)
            result = new Long(total_times);
        else if (type == AdSType.ETotalDevices)
            result = new Long(total_devices);
        else if (type == AdSType.ETotalLocations)
            result = new Long(total_locations);
        else if (type == AdSType.ETotalPauses)
            result = new Long(total_pauses);
        else
            result = created_at;
        return result;
    }

    // @Override
    // protected void addCacheRelationship() {
    // super.addCacheRelationship();
    // ADSAdlist adlist = ADSAdlist.getAdlist(adlist_id);
    // if (adlist != null) {
    // adlist.getAdCacheKeySortSet().add(getCacheKey(id), getCreatedTime());
    // // adlist.onAdUpdate();
    // }
    // }
    //
    // @Override
    // protected void removeCacheRelationship() {
    // ADSAdlist adlist = ADSAdlist.getAdlist(adlist_id);
    // if (adlist != null) {
    // adlist.getAdCacheKeySortSet().remove(getCacheKey(id));
    // // adlist.onAdUpdate();
    // }
    // super.removeCacheRelationship();
    // }

}

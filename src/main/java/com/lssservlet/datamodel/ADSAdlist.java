package com.lssservlet.datamodel;

import java.util.ArrayList;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Transient;

import com.lssservlet.cache.CacheMap;
import com.lssservlet.core.DataManager;
import com.lssservlet.datamodel.ADSDbKey.Type;
import com.lssservlet.utils.JCModel;
import com.lssservlet.utils.JsonField;
import com.lssservlet.utils.SortSet;

@SuppressWarnings("serial")
@Entity
@Access(AccessType.FIELD)
@Table(name = "t_adlist")
@JCModel(type = Type.EAdlist)
public class ADSAdlist extends ADSBase implements Comparable<Object> {
    @Column(columnDefinition = ADSDbKey.Column.VARCHAR64)
    public String name;

    @Column(columnDefinition = ADSDbKey.Column.TEXT)
    public String description;

    @Column(columnDefinition = ADSDbKey.Column.SMALLINT_UNSIGNED
            + " COMMENT '0: full screen image, 1: split screen video and images, 2: full screen video and images'")
    public Integer layout;

    @Column(columnDefinition = ADSDbKey.Column.BIGINT_NOT_NULL + " DEFAULT '15'")
    public Long slide_interval;

    @Column(columnDefinition = ADSDbKey.Column.VARCHAR64)
    public String created_by;

    @Transient
    @JsonField(responseFilter = true)
    public String location_id;

    public static String getCacheKey(String listId) {
        return getCacheKey(Type.EAdlist, listId);
    }

    public static ADSAdlist getAdlist(String listId) {
        return getCache(Type.EAdlist, listId);
    }

    public SortSet getAdCacheKeySortSet() {
        return getOrCreateSortSet(Type.EAd);
    }

    public ArrayList<ADSAd> getAds() {
        return DataManager.getInstance().getMapListObject(getAdCacheKeySortSet());
    }

    public CacheMap<String, String> getLocationMap() {
        return DataManager.getInstance().getOrCreateCacheMap(
                ADSDbKey.Type.EAdlist.getValue() + ":" + ADSDbKey.Type.ELocation.getValue() + ":" + getId());
    }

    public CacheMap<String, String> getAdMap() {
        return DataManager.getInstance().getOrCreateCacheMap(
                ADSDbKey.Type.EAdlist.getValue() + ":" + ADSDbKey.Type.EAd.getValue() + ":" + getId());
    }

    public ADSAdlist onAdUpdate() {
        ArrayList<ADSAd> ads = getAds();
        layout = 0;
        for (ADSAd ad : ads) {
            if (ad.media_type.equalsIgnoreCase("video")) {
                layout = 1;
                break;
            }
        }
        return update(true);
    }

    @Override
    public int compareTo(Object aObj) {
        if (aObj == null || !(aObj instanceof ADSAdlist)) {
            return -1;
        }

        ADSAdlist secondUser = (ADSAdlist) aObj;
        return (int) (secondUser.created_at - created_at);
    }

    public SortSet getAdlistCacheKeySortSet() {
        return DataManager.getInstance().getOrCreateSortSet(ADSDbKey.Type.EAdlist.getValue());
    }

    @Override
    public void addCacheRelationship() {
        getAdlistCacheKeySortSet().add(getCacheKey(), getCreatedTime());
    }

    @Override
    public void removeCacheRelationship() {
        getAdlistCacheKeySortSet().remove(getCacheKey());
    }
}

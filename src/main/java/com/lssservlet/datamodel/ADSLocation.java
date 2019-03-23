package com.lssservlet.datamodel;

import java.util.ArrayList;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

import com.lssservlet.core.DataManager;
import com.lssservlet.datamodel.ADSDbKey.Type;
import com.lssservlet.utils.JCModel;
import com.lssservlet.utils.MapSet;
import com.lssservlet.utils.SortSet;

@SuppressWarnings("serial")
@Entity
@Access(AccessType.FIELD)
@Table(name = "t_location")
@JCModel(type = Type.ELocation)
public class ADSLocation extends ADSBase {
    @Column(columnDefinition = ADSDbKey.Column.VARCHAR64)
    public String name;

    @Column(columnDefinition = ADSDbKey.Column.VARCHAR128_NOT_NULL)
    public String password;

    @Column(columnDefinition = ADSDbKey.Column.VARCHAR64_NOT_NULL)
    public String salt;

    @Column(columnDefinition = ADSDbKey.Column.VARCHAR512_NOT_NULL)
    public String address;

    @Column(columnDefinition = ADSDbKey.Column.VARCHAR64_NOT_NULL)
    public String phone;

    @Column(columnDefinition = ADSDbKey.Column.VARCHAR128)
    public String email;

    @Column(columnDefinition = ADSDbKey.Column.VARCHAR128)
    public String contact;

    @Column(columnDefinition = ADSDbKey.Column.VARCHAR64)
    public String region;

    @Column(columnDefinition = ADSDbKey.Column.VARCHAR64)
    public String active_adlist_id;

    @Column(columnDefinition = ADSDbKey.Column.INT_DEFAULT_ZERO)
    public Integer upgrade;

    @Column(columnDefinition = ADSDbKey.Column.VARCHAR64)
    public String created_by;

    public static String getCacheKey(String locationId) {
        return getCacheKey(Type.ELocation, locationId);
    }

    public static ADSLocation getLocation(String locationId) {
        return getCache(Type.ELocation, locationId);
    }

    public SortSet getAdlistCacheKeySet() {
        return DataManager.getInstance().getOrCreateSortSet(ADSDbKey.Type.ELocation.getValue(), getId(),
                ADSDbKey.Type.EAdlist.getValue());
    }

    public ArrayList<ADSAdlist> getAdlists() {
        return DataManager.getInstance().getMapListObject(getAdlistCacheKeySet(), false);
    }

    public MapSet<String> getDeviceCacheKeySet() {
        return getOrCreateSet(Type.EDevice);
    }

    public ArrayList<ADSDevice> getDevices() {
        return DataManager.getInstance().getMapListObject(getDeviceCacheKeySet());
    }

    public ADSAdlist getActiveAdlist() {
        ADSAdlist result = null;
        if (active_adlist_id != null)
            result = ADSAdlist.getAdlist(active_adlist_id);
        return result;
    }

    public SortSet getLocationCacheKeySortSet() {
        return DataManager.getInstance().getOrCreateSortSet(ADSDbKey.Type.ELocation.getValue());
    }

    @Override
    protected void addCacheRelationship() {
        // super.addCacheRelationship();
        getLocationCacheKeySortSet().add(getCacheKey(), getCreatedTime());
    }

    @Override
    protected void removeCacheRelationship() {
        getLocationCacheKeySortSet().remove(getCacheKey());
        // super.removeCacheRelationship();
    }
}

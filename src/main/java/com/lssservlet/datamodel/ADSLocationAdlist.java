package com.lssservlet.datamodel;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

import com.lssservlet.datamodel.ADSDbKey.Type;
import com.lssservlet.utils.JCModel;

@SuppressWarnings("serial")
@Entity
@Access(AccessType.FIELD)
@Table(name = "t_location_adlist")
@JCModel(type = Type.ELocationAdlist)
public class ADSLocationAdlist extends ADSBase {
    @Column(columnDefinition = ADSDbKey.Column.VARCHAR64_NOT_NULL)
    public String location_id;

    @Column(columnDefinition = ADSDbKey.Column.VARCHAR64_NOT_NULL)
    public String adlist_id;

    @Column(columnDefinition = ADSDbKey.Column.TINYINT_UNSIGNED_DEFAULT_ZERO)
    public Integer active;

    @Column(columnDefinition = ADSDbKey.Column.VARCHAR64_NOT_NULL)
    public String created_by;

    public static String getCacheKey(String aId) {
        return getCacheKey(Type.ELocationAdlist, aId);
    }

    public static ADSLocationAdlist getLocationAdlist(String aId) {
        return getCache(Type.ELocationAdlist, aId);
    }

    @Override
    protected void addCacheRelationship() {
        super.addCacheRelationship();

        ADSLocation location = ADSLocation.getLocation(location_id);
        ADSAdlist adlist = ADSAdlist.getAdlist(adlist_id);
        if (location != null && adlist != null)
            location.getAdlistCacheKeySet().add(ADSAdlist.getCacheKey(adlist_id), adlist.getCreatedTime());

        if (adlist != null)
            adlist.getLocationMap().put(location_id, getCacheKey());
    }

    @Override
    protected void removeCacheRelationship() {
        ADSLocation location = ADSLocation.getLocation(location_id);
        ADSAdlist adlist = ADSAdlist.getAdlist(adlist_id);
        if (location != null && adlist != null)
            location.getAdlistCacheKeySet().remove(ADSAdlist.getCacheKey(adlist_id));

        if (adlist != null)
            adlist.getLocationMap().remove(location_id);

        super.removeCacheRelationship();
    }
}

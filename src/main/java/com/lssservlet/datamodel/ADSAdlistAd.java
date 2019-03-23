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
@Table(name = "t_adlist_ad")
@JCModel(type = Type.EAdlistAd)
public class ADSAdlistAd extends ADSBase {
    @Column(columnDefinition = ADSDbKey.Column.VARCHAR64_NOT_NULL)
    public String adlist_id;

    @Column(columnDefinition = ADSDbKey.Column.VARCHAR64_NOT_NULL)
    public String ad_id;

    @Column(columnDefinition = ADSDbKey.Column.VARCHAR64)
    public String created_by;

    public static String getCacheKey(String aId) {
        return getCacheKey(Type.EAdlistAd, aId);
    }

    public static ADSAdlistAd getAdlistAd(String aId) {
        return getCache(Type.EAdlistAd, aId);
    }

    @Override
    protected void addCacheRelationship() {
        super.addCacheRelationship();

        ADSAd ad = ADSAd.getAd(ad_id);
        ADSAdlist adlist = ADSAdlist.getAdlist(adlist_id);
        if (ad != null && adlist != null)
            adlist.getAdCacheKeySortSet().add(ADSAd.getCacheKey(ad_id), getCreatedTime());

        if (adlist != null)
            adlist.getAdMap().put(ad_id, getCacheKey());
    }

    @Override
    protected void removeCacheRelationship() {
        ADSAd ad = ADSAd.getAd(ad_id);
        ADSAdlist adlist = ADSAdlist.getAdlist(adlist_id);
        if (ad != null && adlist != null)
            adlist.getAdCacheKeySortSet().remove(ADSAd.getCacheKey(ad_id));

        if (adlist != null)
            adlist.getAdMap().remove(ad_id);

        super.removeCacheRelationship();
    }
}

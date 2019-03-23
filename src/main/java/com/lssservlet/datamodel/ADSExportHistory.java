package com.lssservlet.datamodel;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

import com.lssservlet.core.DataManager;
import com.lssservlet.datamodel.ADSDbKey.Type;
import com.lssservlet.utils.JCModel;
import com.lssservlet.utils.JsonObject;
import com.lssservlet.utils.SortSet;

@SuppressWarnings("serial")
@Entity
@Access(AccessType.FIELD)
@Table(name = "t_export_history")
@JCModel(type = Type.EExportHistory)
public class ADSExportHistory extends ADSBase {
    @Column(columnDefinition = ADSDbKey.Column.VARCHAR64)
    public String created_by;

    @Column(columnDefinition = ADSDbKey.Column.VARCHAR32)
    public String type;

    @Column(columnDefinition = ADSDbKey.Column.VARCHAR512)
    public String request;

    @Column(columnDefinition = ADSDbKey.Column.VARCHAR256)
    public String url;

    @Column(columnDefinition = ADSDbKey.Column.TINYINT_UNSIGNED_DEFAULT_ZERO)
    public Integer uploaded = 0;

    static public ADSExportHistory fromJson(JsonObject data) {
        return (ADSExportHistory) ADSData.fromJson(data);
    }

    public static ADSExportHistory getExportHistory(String exportId) {
        return getCache(Type.EExportHistory, exportId);
    }

    public static String getCacheKey(String exportId) {
        return ADSBase.getCacheKey(Type.EExportHistory, exportId);
    }

    public SortSet getExportCacheKeySortSet() {
        return DataManager.getInstance().getOrCreateSortSet(ADSDbKey.Type.EExportHistory.getValue());
    }

    @Override
    protected void addCacheRelationship() {
        // super.addCacheRelationship();
        getExportCacheKeySortSet().add(getCacheKey(), created_at);
    }

    @Override
    protected void removeCacheRelationship() {
        getExportCacheKeySortSet().remove(getCacheKey());
        // super.removeCacheRelationship();
    }
}
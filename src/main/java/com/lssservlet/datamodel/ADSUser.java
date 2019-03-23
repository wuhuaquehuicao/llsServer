package com.lssservlet.datamodel;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Transient;

import com.lssservlet.core.DataManager;
import com.lssservlet.datamodel.ADSDbKey.Type;
import com.lssservlet.utils.JCModel;
import com.lssservlet.utils.JsonObject;
import com.lssservlet.utils.SortSet;

@SuppressWarnings("serial")
@Entity
@Access(AccessType.FIELD)
@Table(name = "t_user")
@JCModel(type = Type.EUser)
public class ADSUser extends ADSBase {// implements Comparable<Object> {
    @Column(columnDefinition = ADSDbKey.Column.VARCHAR64_NOT_NULL)
    public String name;

    @Column(columnDefinition = ADSDbKey.Column.VARCHAR64_NOT_NULL)
    public String salt;

    @Column(columnDefinition = ADSDbKey.Column.VARCHAR128_NOT_NULL)
    public String password;

    @Column(columnDefinition = ADSDbKey.Column.VARCHAR64 + "DEFAULT 'user'")
    public String role;

    @Column(columnDefinition = ADSDbKey.Column.VARCHAR64)
    public String created_by;

    @Column(columnDefinition = ADSDbKey.Column.TINYINT_UNSIGNED_DEFAULT_ZERO)
    public Integer original_pwd = 0;

    @Transient
    public String token;

    static public ADSUser fromJson(JsonObject data) {
        return (ADSUser) ADSData.fromJson(data);
    }

    public static ADSUser getUser(String userId) {
        return getCache(Type.EUser, userId);
    }

    public SortSet getAdCacheKeySortSet() {
        return DataManager.getInstance().getOrCreateSortSet(ADSDbKey.Type.EUser.getValue());
    }

    @Override
    protected void addCacheRelationship() {
        // super.addCacheRelationship();
        getAdCacheKeySortSet().add(getCacheKey(), created_at);
        DataManager.getInstance().getUserNameMap().put(name, getId());
    }

    @Override
    protected void removeCacheRelationship() {
        getAdCacheKeySortSet().remove(getCacheKey());
        DataManager.getInstance().getUserNameMap().remove(name);
        // super.removeCacheRelationship();
    }

    public boolean isAdminUser() {
        return (role != null && role.equals(ADSDbKey.Role_Admin)) ? true : false;
    }
}

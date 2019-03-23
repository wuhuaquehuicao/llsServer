package com.lssservlet.datamodel;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.Table;

import com.lssservlet.core.DataManager;
import com.lssservlet.datamodel.ADSDbKey.EventType;
import com.lssservlet.datamodel.ADSDbKey.Type;
import com.lssservlet.utils.JCModel;
import com.lssservlet.utils.JsonObject;
import com.lssservlet.utils.SortSet;

@SuppressWarnings("serial")
@Entity
@Access(AccessType.FIELD)
@Table(name = "t_device")
@JCModel(type = Type.EDevice)
public class ADSDevice extends ADSBase {
    @Column(columnDefinition = ADSDbKey.Column.VARCHAR64)
    public String name;

    @Column(columnDefinition = ADSDbKey.Column.VARCHAR64)
    public String location_id;

    @Column(columnDefinition = ADSDbKey.Column.VARCHAR64)
    public String password;

    @Column(columnDefinition = ADSDbKey.Column.VARCHAR64)
    public String version;

    @Column(columnDefinition = ADSDbKey.Column.VARCHAR64)
    public String model;

    @Column(columnDefinition = ADSDbKey.Column.VARCHAR64)
    public String battery_health = null;

    @Column(columnDefinition = ADSDbKey.Column.SMALLINT_UNSIGNED + " DEFAULT '0'")
    public Integer battery_level = 0;

    @Column(columnDefinition = ADSDbKey.Column.TINYINT_UNSIGNED_DEFAULT_ZERO)
    public Integer active = 0;

    @Convert(converter = DateConverter.class, disableConversion = false)
    @Column(name = "last_active_time", columnDefinition = ADSDbKey.Column.DATETIME)
    public Long last_active_time;

    @Column(name = "log", columnDefinition = ADSDbKey.Column.TINYINT_UNSIGNED_DEFAULT_ZERO)
    public Integer log = 0;

    static public ADSDevice fromJson(JsonObject data) {
        return (ADSDevice) ADSData.fromJson(data);
    }

    public static ADSDevice getDevice(String deviceId) {
        return getCache(Type.EDevice, deviceId);
    }

    public static String getCacheKey(String deviceId) {
        return ADSBase.getCacheKey(Type.EDevice, deviceId);
    }

    public SortSet getDeviceCacheKeySortSet() {
        return DataManager.getInstance().getOrCreateSortSet(ADSDbKey.Type.EDevice.getValue());
    }

    @Override
    protected void addCacheRelationship() {
        // super.addCacheRelationship();
        getDeviceCacheKeySortSet().add(getCacheKey(), updated_at);

        ADSLocation location = ADSLocation.getLocation(location_id);
        if (location != null)
            location.getDeviceCacheKeySet().add(getCacheKey());
    }

    @Override
    protected void removeCacheRelationship() {
        ADSLocation location = ADSLocation.getLocation(location_id);
        if (location != null)
            location.getDeviceCacheKeySet().remove(getCacheKey());

        getDeviceCacheKeySortSet().remove(getCacheKey());
        // super.removeCacheRelationship();
    }

    public void setPassword(String aPassword, String adminId) {
        if (aPassword == null || aPassword.length() == 0)
            return;

        String currentPassword = password;
        password = aPassword;

        if (!aPassword.equals(currentPassword)) {
            JsonObject data = new JsonObject();
            data.put("old", currentPassword);
            data.put("new", aPassword);
            DataManager.getInstance().addEvent(EventType.EDevicePasswordChanged, getId(), adminId, null,
                    data.toString());
        }
    }

    public void setLocationId(String aLocationId, String adminId) {
        String currentLocationId = location_id;
        location_id = aLocationId;
        if ((aLocationId == null && currentLocationId != null)
                || (aLocationId != null && !aLocationId.equals(currentLocationId))) {
            JsonObject data = new JsonObject();
            data.put("old", currentLocationId);
            data.put("new", aLocationId);
            DataManager.getInstance().addEvent(EventType.EDeviceLocationChanged, getId(), adminId, null,
                    data.toString());

            ADSLocation oldLocation = ADSLocation.getLocation(currentLocationId);
            if (oldLocation != null) {
                oldLocation.getDeviceCacheKeySet().remove(ADSDevice.getCacheKey(getId()));
            }
        }
    }
}
package com.lssservlet.managers;

import java.util.ArrayList;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.lssservlet.core.DataManager;
import com.lssservlet.datamodel.ADSDbKey;
import com.lssservlet.datamodel.ADSDbKey.EventType;
import com.lssservlet.datamodel.ADSDbKey.Type;
import com.lssservlet.datamodel.ADSDevice;
import com.lssservlet.exception.ErrorCode;
import com.lssservlet.utils.DataException;
import com.lssservlet.utils.JsonObject;

public class DeviceManager extends BaseManager {
    protected static final Logger log = LogManager.getLogger(DeviceManager.class);
    private static volatile DeviceManager sInstance = null;

    private DeviceManager() {
    }

    public static DeviceManager getInstance() {
        if (sInstance == null) {
            synchronized (DeviceManager.class) {
                if (sInstance == null) {
                    sInstance = new DeviceManager();
                }
            }
        }
        return sInstance;
    }

    public ArrayList<ADSDevice> getDevices() throws DataException {
        ArrayList<ADSDevice> result = null;
        result = DataManager.getInstance().getDataList(ADSDbKey.Type.EDevice);
        // result = ResultFilter.filter(result, filter, limit, offset);
        return result;
    }

    public ArrayList<ADSDevice> getDevices(boolean ascending) {
        return DataManager.getInstance().getSortDataList(Type.EDevice, ascending);
    }

    public ADSDevice createDevice(String deviceId, String name, String locationId) throws DataException {
        if (deviceId == null || deviceId.isEmpty())
            throw new DataException(ErrorCode.DEVICE_INVALID_MAC, "Mac error.");
        String did = deviceId.toLowerCase();
        ADSDevice result = ADSDevice.getDevice(did);
        if (result != null)
            return result;
        // throw new DataException(ErrorCode.DEVICE_DUPLICATED_MAC, "Duplicated mac: " + deviceId);
        result = new ADSDevice();
        result.id = did;
        result.name = name;
        result.password = "borders";
        result.flag = 0;
        result.created_at = DataManager.getInstance().dbtime();
        result.updated_at = result.created_at;
        if (locationId != null)
            result.location_id = locationId;
        result.update(true);
        return result;
    }

    public ADSDevice getDevice(String deviceId) throws DataException {
        String did = (deviceId != null) ? deviceId.toLowerCase() : null;
        if (did == null || did.isEmpty())
            throw new DataException(ErrorCode.DEVICE_INVALID_MAC, "Mac error.");

        ADSDevice result = ADSDevice.getDevice(did);
        if (result == null)
            throw new DataException(ErrorCode.DEVICE_NOT_FOUND_DEVICE, "Not found device: " + deviceId);
        return result;
    }

    public ADSDevice updateDevice(String deviceId, String name, String locationId, Integer batteryLevel,
            String batteryHealth, String version, String model, String password, String adminId) throws DataException {
        String did = (deviceId != null) ? deviceId.toLowerCase() : null;
        if (did == null || did.isEmpty())
            throw new DataException(ErrorCode.DEVICE_INVALID_MAC, "Mac error.");

        ADSDevice result = ADSDevice.getDevice(did);
        if (result == null)
            throw new DataException(ErrorCode.DEVICE_NOT_FOUND_DEVICE, "Not found device: " + did);
        String oldVersion = result.version;

        if (name != null)
            result.name = name;
        result.setLocationId(locationId, adminId);
        if (batteryLevel != null)
            result.battery_level = batteryLevel;
        if (batteryHealth != null)
            result.battery_health = batteryHealth;
        if (version != null)
            result.version = version;

        result.setPassword(password, adminId);
        if (model != null)
            result.model = model;
        if (adminId == null) {
            result.active = 1;
            result.updated_at = DataManager.getInstance().dbtime();
        }
        result.update(true);

        if (version != null && !version.equals(oldVersion)) {
            JsonObject data = new JsonObject();
            data.put("old", oldVersion);
            data.put("new", version);
            DataManager.getInstance().addEvent(EventType.EAppUpgraded, did, adminId, null, data.toString());
        }

        return result;
    }
}

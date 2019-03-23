package com.lssservlet.managers;

import java.util.ArrayList;
import java.util.Random;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.lssservlet.core.DataManager;
import com.lssservlet.datamodel.ADSAdlist;
import com.lssservlet.datamodel.ADSDbKey;
import com.lssservlet.datamodel.ADSDbKey.EventType;
import com.lssservlet.datamodel.ADSDbKey.Type;
import com.lssservlet.datamodel.ADSDevice;
import com.lssservlet.datamodel.ADSLocation;
import com.lssservlet.datamodel.ADSLocationAdlist;
import com.lssservlet.exception.ErrorCode;
import com.lssservlet.utils.AlphaId;
import com.lssservlet.utils.DataException;
import com.lssservlet.utils.JsonObject;

public class LocationManager extends BaseManager {
    private static volatile LocationManager sInstance = null;
    private static int HASH_STRING_LENGTH = 20;
    protected static final Logger log = LogManager.getLogger(LocationManager.class);

    private LocationManager() {
    }

    public static LocationManager getInstance() {
        if (sInstance == null) {
            synchronized (LocationManager.class) {
                if (sInstance == null) {
                    sInstance = new LocationManager();
                }
            }
        }
        return sInstance;
    }

    public ArrayList<ADSLocation> getLocations() throws DataException {
        return DataManager.getInstance().getDataList(ADSDbKey.Type.ELocation);
    }

    public ArrayList<ADSLocation> getLocations(boolean ascending) {
        return DataManager.getInstance().getSortDataList(Type.ELocation, ascending);
    }

    public ADSLocation createLocation(String name, String password, String address, String phone, String email,
            String contact, String activeAdlistId, String adminId) throws DataException {
        if (address == null || address.isEmpty() || phone == null || phone.isEmpty())
            throw new DataException(ErrorCode.LOCATION_INVALID_LOCATION, "Invalid address or phone");
        if (activeAdlistId != null && activeAdlistId.length() > 0) {
            try {
                AdlistManager.getInstance().getAdlist(activeAdlistId);
            } catch (DataException e) {
                throw new DataException(ErrorCode.ADLIST_INVALID_ADLIST, "Invalid adlist.");
            }
        }

        ADSLocation result = new ADSLocation();
        result.id = "loc_" + AlphaId.generateID();
        if (name != null)
            result.name = name;
        result.password = "borders";
        result.address = address;
        result.phone = phone;
        if (email != null)
            result.email = email;
        result.flag = 0;
        if (contact != null)
            result.contact = contact;
        result.created_at = DataManager.getInstance().dbtime();
        result.updated_at = result.created_at;
        result.created_by = adminId;
        result.upgrade = 0;
        result.update(true);

        if (activeAdlistId != null && activeAdlistId.length() > 0) {
            result = updateLocation(result.getId(), name, address, activeAdlistId, email, contact, phone, null,
                    adminId);
        }

        return result;
    }

    public ADSLocation updateLocation(String locationId, String name, String address, String activeAdlistId,
            String email, String contact, String phone, String password, String adminId) throws DataException {

        if (locationId == null || locationId.isEmpty())
            throw new DataException(ErrorCode.LOCATION_INVALID_LOCATION, "Invalid location.");

        ADSLocation result = ADSLocation.getLocation(locationId);
        if (result == null)
            throw new DataException(ErrorCode.LOCATION_NOT_FOUND_LOCATION, "Not found location: " + locationId);
        if (name != null)
            result.name = name;
        if (address != null)
            result.address = address;
        if (phone != null)
            result.phone = phone;
        if (password != null && password.length() > 0) {
            String oldPwd = result.password;
            result.password = password;
            if (!oldPwd.equals(result.password)) {
                ArrayList<ADSDevice> devices = result.getDevices();
                if (devices.size() > 0) {
                    for (ADSDevice d : devices) {
                        d.setPassword(password, adminId);
                        d.update(true);
                    }
                }
            }
        }

        ADSAdlist adlist = null;
        String oldActiveAdlistId = result.active_adlist_id;

        if (activeAdlistId != null) {
            try {
                adlist = AdlistManager.getInstance().getAdlist(activeAdlistId);
            } catch (DataException e) {
                log.warn("not found adlist: " + activeAdlistId);
            }
            if (activeAdlistId.length() == 0) {
                result.active_adlist_id = null;
            } else {
                result.active_adlist_id = activeAdlistId;
            }
        }

        if ((activeAdlistId == null && oldActiveAdlistId != null)
                || (activeAdlistId != null && !activeAdlistId.equals(oldActiveAdlistId))) {

            if (adlist != null && adlist.getLocationMap().get(locationId) == null)
                LocationAdlistManager.getInstance().createLocationAdlist(locationId, activeAdlistId, 1, adminId);

            ADSAdlist oldAdlist = null;
            try {
                oldAdlist = AdlistManager.getInstance().getAdlist(oldActiveAdlistId);
            } catch (DataException e) {
                log.warn("not found old adlist: " + oldActiveAdlistId);
            }

            if (oldAdlist != null) {
                String oldLaCacheKey = oldAdlist.getLocationMap().get(locationId);
                ADSLocationAdlist oldla = DataManager.getInstance().getCache(oldLaCacheKey);
                if (oldla != null) {
                    oldla.delete(true);
                } else {
                    log.info(
                            "not found local-adlist for location: " + locationId + ", oldLaCacheKey: " + oldLaCacheKey);
                }
            }

            JsonObject data = new JsonObject();
            data.put("old", oldActiveAdlistId);
            data.put("new", activeAdlistId);
            DataManager.getInstance().addEvent(EventType.ELocationActiveAdlistChanged, locationId, adminId, null,
                    data.toString());
        }

        if (email != null)
            result.email = email;
        if (contact != null)
            result.contact = contact;
        result.updated_at = DataManager.getInstance().dbtime();
        result.update(true);
        return result;

    }

    public void addDevices(String locationId, ArrayList<String> deviceIds, String adminId) {
        if (locationId == null || ADSLocation.getLocation(locationId) == null)
            return;
        if (deviceIds.size() > 0) {
            for (String did : deviceIds) {
                ADSDevice device = ADSDevice.getDevice(did.toLowerCase());
                if (device != null) {
                    device.setLocationId(locationId, adminId);
                    device.update(true);
                }
            }
        }
    }

    // public ADSLocation createLocationAdlists(String locationId, ArrayList<String> adlistIds) throws DataException {
    //
    // if (locationId == null || locationId.isEmpty())
    // throw new DataException(ErrorCode.LOCATION_INVALID_LOCATION, "Invalid location.");
    //
    // ADSLocation result = ADSLocation.getLocation(locationId);
    // if (result == null)
    // throw new DataException(ErrorCode.LOCATION_NOT_FOUND_LOCATION, "Not found location: " + locationId);
    // if (adlistIds != null && adlistIds.size() > 0) {
    // for (String adlistId : adlistIds) {
    // ADSAdlist adlist = null;
    // try {
    // adlist = AdlistManager.getInstance().getAdlist(adlistId);
    // } catch (DataException e) {
    // // TODO: handle exception
    // log.warn("not found adlist: " + adlistId);
    // // throw new DataException(ErrorCode.ADLIST_NOT_FOUND_ADLIST, "Not found adlist.");
    // }
    // result.active_adlist_id = activeAdlistId;
    // if (adlist != null && adlist.getLocationMap().get(locationId) == null)
    // LocationAdlistManager.getInstance().createLocationAdlist(locationId, activeAdlistId, 1);
    //
    // ADSAdlist oldAdlist = null;
    // try {
    // oldAdlist = AdlistManager.getInstance().getAdlist(oldActiveAdlistId);
    // } catch (DataException e) {
    // // TODO: handle exception
    // log.warn("not found old adlist: " + oldActiveAdlistId);
    // // throw new DataException(ErrorCode.ADLIST_NOT_FOUND_ADLIST, "Not found adlist.");
    // }
    //
    // if (oldAdlist != null) {
    // String oldLaCacheKey = oldAdlist.getLocationMap().get(locationId);
    // ADSLocationAdlist oldla = DataManager.getInstance().getCache(oldLaCacheKey);
    // oldla.delete(true);
    // }
    // }
    // }
    //
    // if (email != null)
    // result.email = email;
    // if (contact != null)
    // result.contact = contact;
    // result.updated_at = DataManager.getInstance().dbtime();
    // result.update(true);
    // return result;
    //
    // }

    private static String generatSalt() {
        String hash = "";
        for (int index = 0; index < HASH_STRING_LENGTH; index++) {
            Random random = new Random();
            Integer value = Math.abs(random.nextInt() % 10);
            hash += String.format("%d", value);
        }
        return hash;
    }
}

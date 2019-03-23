package com.lssservlet.managers;

import java.util.ArrayList;

import com.lssservlet.core.DataManager;
import com.lssservlet.datamodel.ADSDbKey;
import com.lssservlet.datamodel.ADSLocation;
import com.lssservlet.datamodel.ADSLocationAdlist;
import com.lssservlet.exception.ErrorCode;
import com.lssservlet.utils.AlphaId;
import com.lssservlet.utils.DataException;

public class LocationAdlistManager extends BaseManager {
    private static volatile LocationAdlistManager sInstance = null;
    private static int HASH_STRING_LENGTH = 20;

    private LocationAdlistManager() {
    }

    public static LocationAdlistManager getInstance() {
        if (sInstance == null) {
            synchronized (LocationAdlistManager.class) {
                if (sInstance == null) {
                    sInstance = new LocationAdlistManager();
                }
            }
        }
        return sInstance;
    }

    public ArrayList<ADSLocation> getLocations() throws DataException {
        return DataManager.getInstance().getDataList(ADSDbKey.Type.ELocation);
    }

    public ADSLocationAdlist createLocationAdlist(String locationId, String adlistId, Integer active, String adminId)
            throws DataException {
        if (locationId == null || locationId.isEmpty() || adlistId == null || adlistId.isEmpty() || active == null)
            throw new DataException(ErrorCode.LOCATION_INVALID_LOCATION, "Invalid password, address or phone");

        ADSLocationAdlist result = new ADSLocationAdlist();
        result.id = "la_" + AlphaId.generateID();
        result.location_id = locationId;
        result.adlist_id = adlistId;
        result.active = active;
        result.flag = 0;
        result.created_by = adminId;
        result.created_at = DataManager.getInstance().dbtime();
        result.updated_at = result.created_at;
        result.update(true);
        return result;
    }
}

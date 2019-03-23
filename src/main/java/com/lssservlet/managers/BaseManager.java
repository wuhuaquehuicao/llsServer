package com.lssservlet.managers;

import com.lssservlet.api.HandleBase.FlagParams;
import com.lssservlet.core.DataManager;
import com.lssservlet.datamodel.ADSBase;
import com.lssservlet.datamodel.ADSData;
import com.lssservlet.exception.ErrorCode;
import com.lssservlet.utils.DataException;

public abstract class BaseManager {

    protected void checkFlagParams(FlagParams params) throws DataException {
        if (params.id == null || params.id.length() == 0 || params.flag < 0 || params.flag > 1) {
            throw new DataException(ErrorCode.BAD_REQUEST, "Invalid post data");
        }
    }

    protected <M extends ADSBase> M updateFlag(M data, String userId, int flag) {
        if (data.flag != flag) {
            data.flag = flag;
            data.updated_at = DataManager.getInstance().dbtime();
            data.update(true);
        }

        return data;
    }

    protected <M extends ADSData> M checkExist(String cacheKey, boolean checkActivate) throws DataException {
        M data = DataManager.getInstance().getCacheIgnoreLock(cacheKey);
        if (data == null) {
            throw new DataException(ErrorCode.CACHEKEY_NOT_FOUND, "Not found cacheKey: " + cacheKey);
        }

        if (checkActivate) {
            checkActivate(data);
        }

        return data;
    }

    protected void checkConflict(String originalId, String comparedId, String msg) throws DataException {
        if (originalId != null && !originalId.equals(comparedId)) {
            throw new DataException(ErrorCode.CONFLICT, msg);
        }
    }

    protected void checkConflict(Integer originalId, Integer comparedId, String msg) throws DataException {
        if (originalId != null && !originalId.equals(comparedId)) {
            throw new DataException(ErrorCode.CONFLICT, msg);
        }
    }

    private void checkActivate(ADSData data) throws DataException {
        if (data.flag == 1) {
            throw new DataException(ErrorCode.FORBIDDEN, "cacheKey: " + data.getCacheKey() + " - not activate");
        }
    }
}

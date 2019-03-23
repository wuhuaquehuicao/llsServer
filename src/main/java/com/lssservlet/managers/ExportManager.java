package com.lssservlet.managers;

import java.io.File;
import java.util.ArrayList;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.lssservlet.api.HandleBase.QueryParams1;
import com.lssservlet.api.HandleExports.ExportRequestParams;
import com.lssservlet.core.Config;
import com.lssservlet.core.DataManager;
import com.lssservlet.datamodel.ADSAd;
import com.lssservlet.datamodel.ADSData;
import com.lssservlet.datamodel.ADSDbKey;
import com.lssservlet.datamodel.ADSDbKey.Type;
import com.lssservlet.datamodel.ADSExportHistory;
import com.lssservlet.exception.ErrorCode;
import com.lssservlet.service.AWSS3Service;
import com.lssservlet.utils.AlphaId;
import com.lssservlet.utils.CSVUtil;
import com.lssservlet.utils.DataException;
import com.lssservlet.utils.JsonArray;
import com.lssservlet.utils.ResultFilter;
import com.lssservlet.utils.TaskManager;

public class ExportManager extends BaseManager {
    protected static final Logger log = LogManager.getLogger(ExportManager.class);
    private static volatile ExportManager sInstance = null;

    private ExportManager() {
    }

    public static ExportManager getInstance() {
        if (sInstance == null) {
            synchronized (ExportManager.class) {
                if (sInstance == null) {
                    sInstance = new ExportManager();
                }
            }
        }
        return sInstance;
    }

    public ArrayList<ADSExportHistory> getExports(boolean ascending) {
        return DataManager.getInstance().getSortDataList(Type.EExportHistory, ascending);
    }

    public ADSExportHistory createExport(ExportRequestParams request, String created_by) throws DataException {
        JsonArray data = null;
        try {
            data = search(request);
            if (data == null || data.size() == 0)
                throw new DataException(ErrorCode.EXPORT_INVALID_EXPORT, "Not found data.");
        } catch (Exception e) {
            // TODO: handle exception
            throw new DataException(ErrorCode.EXPORT_INVALID_EXPORT, "Not found data.");
        }
        ADSExportHistory result = new ADSExportHistory();
        result.id = "eh_" + AlphaId.generateID();
        result.type = (request.getType() != null) ? request.getType().getValue() : "";
        result.request = request.toString();
        result.created_by = created_by;
        result.uploaded = 0;
        result.flag = 0;
        result.created_at = DataManager.getInstance().dbtime();
        result.updated_at = result.created_at;
        result.update(true);

        final JsonArray dataArray = data;
        TaskManager.runTaskOnThreadPool("exportTask", 5, handler -> {
            try {
                String fileName = Config.getInstance().getHomePath() + "/exports/" + result.id + ".csv";
                CSVUtil.exportDataToCSV(fileName, dataArray);
                File file = new File(fileName);
                AWSS3Service.uploadFile(Config.getInstance().getAwsBucket(),
                        Config.getInstance().getAwsExportBucketKey() + "/" + result.getId() + ".csv", file);

                ADSExportHistory eh = getExport(result.id);
                eh.uploaded = 1;
                eh.updated_at = DataManager.getInstance().dbtime();
                eh.url = Config.getInstance().getAwsCloudFrontBaseUrl() + Config.getInstance().getAwsExportBucketKey()
                        + "/" + result.getId() + ".csv";
                eh.update(true);
            } catch (Exception e) {
                // TODO: handle exception
                log.error(e);
            }
        });
        return result;
    }

    public ADSExportHistory getExport(String exportId) throws DataException {
        if (exportId == null || exportId.isEmpty())
            throw new DataException(ErrorCode.EXPORT_INVALID_EXPORT, "Not found export.");

        ADSExportHistory result = ADSExportHistory.getExportHistory(exportId);
        if (result == null)
            throw new DataException(ErrorCode.EXPORT_INVALID_EXPORT, "Not found export: " + exportId);
        return result;
    }

    @SuppressWarnings("unchecked")
    private JsonArray search(ExportRequestParams request) throws DataException {
        JsonArray result = new JsonArray();
        if (request.clauses != null && request.clauses.size() > 0 || request.order != null || request.desc != null) {
            QueryParams1 params = new QueryParams1();
            ADSDbKey.Type type = request.getType();

            if (request.limit != null) {
                params.limit = request.limit;
            } else {
                params.limit = 100;
            }
            if (request.offset != null) {
                params.offset = request.offset;
            } else {
                params.offset = 0;
            }
            params.orders = new ArrayList<>();
            if (request.order != null)
                params.orders.add(request.order);
            params.clauses = new ArrayList<>();
            for (String c : request.clauses)
                params.clauses.add(c);
            params.or = (request.or != null) ? Boolean.parseBoolean(request.or) : true;
            params.desc = (request.desc == null || request.desc == 0) ? false : true;

            if (type == Type.EAdStatic) {
                ArrayList<ADSAd> data = AdlistManager.getInstance().getAdStatics(request.from, request.to,
                        request.order, request.desc);
                data = ResultFilter.filter(data, null, (request.limit != null) ? request.limit : 100,
                        (request.offset != null) ? request.offset : 0, false);
                for (ADSAd ad : data)
                    result.add(ad.toJsonObject());
            } else {
                params.type = type.getValue();
                ArrayList<ADSData> data = (ArrayList<ADSData>) DataManager.getInstance().queryFromDatabase(params);
                for (ADSData d : data)
                    result.add(d.toJsonObject());
            }
        }

        return result;
    }
}

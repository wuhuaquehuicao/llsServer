package com.lssservlet.datamodel;

import java.io.IOException;
import java.io.Serializable;
import java.util.Date;
import java.util.Map;

import javax.persistence.AttributeConverter;
import javax.persistence.Column;
import javax.persistence.Converter;
import javax.persistence.EntityManager;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import javax.persistence.Transient;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.lssservlet.cache.CacheMap;
import com.lssservlet.core.DataManager;
import com.lssservlet.datamodel.ADSDbKey.Type;
import com.lssservlet.db.JCPersistence;
import com.lssservlet.db.JCPersistence.TransactionHandler;
import com.lssservlet.exception.ErrorCode;
import com.lssservlet.rest.RequestThreadLocal;
import com.lssservlet.rest.ResponseFilter;
import com.lssservlet.utils.DataException;
import com.lssservlet.utils.JCModel;
import com.lssservlet.utils.Json;
import com.lssservlet.utils.Json.JsonBeanPropertyWriter;
import com.lssservlet.utils.Json.JsonData;
import com.lssservlet.utils.JsonField;
import com.lssservlet.utils.JsonObject;
import com.lssservlet.utils.MapSet;
import com.lssservlet.utils.SortSet;

@SuppressWarnings("serial")
@MappedSuperclass
public abstract class ADSData extends JsonData implements Serializable, TransactionHandler {

    protected static final Logger log = LogManager.getLogger(ADSData.class);
    public static final String NAME_ID = "name";

    @Id
    @Column(name = "id", columnDefinition = ADSDbKey.Column.VARCHAR64_NOT_NULL_UNIQUE_KEY)
    public String id;

    @JsonField(responseFilter = true)
    @Column(name = "flag", columnDefinition = ADSDbKey.Column.TINYINT_UNSIGNED_DEFAULT_ZERO)
    public Integer flag = 0;

    @JsonField(responseFilter = true)
    @Transient
    private String data_type;

    @Converter(autoApply = false)
    public static class DateConverter implements AttributeConverter<Long, Date> {
        @Override
        public Date convertToDatabaseColumn(Long date) {
            if (date != null && date > 0L) {
                return new Date(date);
            }
            return null;
        }

        @Override
        public Long convertToEntityAttribute(Date date) {
            if (date != null) {
                return date.getTime();
            }
            return null;
        }
    }

    public ADSData() {
        JCModel[] models = this.getClass().getAnnotationsByType(JCModel.class);
        if (models != null && models.length > 0) {
            JCModel model = models[0];
            data_type = model.type().getValue();
        }
    }

    public String getId() {
        return id;
    }

    static public ADSData fromJson(JsonObject data) {
        if (data == null)
            return null;
        try {
            // load from cache first
            ADSData result = null;
            ADSDbKey.Type t = ADSDbKey.Type.fromString(data.getString(ADSDbKey.TBase.DATA_TYPE));
            result = DataManager.getInstance().getCache(t.getValue() + ":" + data.getString(ADSDbKey.TBase.ID));
            if (result == null) {
                String dataString = data.toString();
                result = Json.mapper.readerFor(JCPersistence.getInstance().getModelClass(t.getValue()))
                        .readValue(dataString);// data.toString());
            } else
                result.merge(data);
            if (result != null) {
                return result;
            } else {
                log.warn("fromJson == null " + data.toString());
            }
        } catch (IOException e) {
            log.error("load data error", e);
        }
        return null;
    }

    private void saveToCacheMap(boolean saveToDatabase) {
        CacheMap<String, ADSData> cacheMap = DataManager.getInstance().getCacheMap(getCacheKey());
        if (cacheMap != null) {
            if (saveToDatabase) {
                cacheMap.put(getCacheKey(), this);
            } else {
                cacheMap.putSkipStore(getCacheKey(), this);
            }
        }
    }

    private void removeFromCacheMap(boolean updateDatabase) {
        CacheMap<String, ADSData> cacheMap = DataManager.getInstance().getCacheMap(getCacheKey());
        if (cacheMap != null) {
            if (updateDatabase) {
                cacheMap.remove(getCacheKey(), this);
            } else {
                cacheMap.remove(getCacheKey());
            }
        }
    }

    private boolean hasChanged() {
        boolean changed = true;
        ADSData od = getCacheData();
        if (od != null) {
            String reason = od.toJsonObject().notEqualsReason(toJsonObject());
            if (reason != null) {
                log.warn("update cache:{}, changed:{}", getCacheKey(), reason);
            } else {
                changed = false;
            }
        } else {
            try {
                String reason = Json.encodePretty(this);
                log.warn("update cache:{}, new:{}", getCacheKey(), reason);
            } catch (Exception e) {
            }
        }

        return changed;
    }

    @Override
    public void beginExecuteTranscation() {
        resetCacheRelationship();
    }

    @Override
    public void executeTransaction(EntityManager em) {
        em.merge(this);
    }

    @Override
    public void transactionFinished() {
        addCacheRelationship();
        saveToCacheMap(false);
    }

    @SuppressWarnings("unchecked")
    public <M extends ADSData> M add(boolean save) {
        resetCacheRelationship();
        if (save) {
            if (getCacheKey() != null) {
                if (!hasChanged()) { // when value hasn't changed, it will fail to save/update to database
                    return (M) this;
                }
                saveToCacheMap(true);
            } else {
                JCPersistence.getInstance().persist(this);
                saveToCacheMap(false);
            }
        }
        addCacheRelationship();
        return (M) this;
    }

    @SuppressWarnings("unchecked")
    public <M extends ADSData> M update(boolean save) {
        resetCacheRelationship();
        if (save) {
            if (getCacheKey() != null) {
                if (hasChanged()) { // when value hasn't changed, it will fail to save/update to database
                    saveToCacheMap(true);
                }
            }
        }
        addCacheRelationship();
        return (M) this;
    }

    public void delete(boolean save) {
        removeCacheRelationship();
        if (save) {
            flag = 1;
            if (hasChanged()) {
                removeFromCacheMap(true);
            }
        }
    }

    public Long getCreatedTime() {
        return 0L;
    }

    @SuppressWarnings("unchecked")
    public <M extends ADSData> M model() {
        return (M) this;
    }

    protected boolean notEqual(String cache, String value) {
        if ((cache == null && value == null) || (cache != null && cache.length() > 0 && cache.equals(value))
                || (value != null && value.length() > 0 && value.equals(cache))) {
            return false;
        }

        return true;
    }

    protected void removeFromCacheMap(String id, String cache, String value) {
        if (notEqual(cache, value)) {
            removeFromCacheMap(id, cache);
        }
    }

    protected void resetCacheRelationship() {
    }

    protected void addCacheRelationship() {
        DataManager.getInstance().addToCacheSet(getDataType(), getCacheKey());
    }

    protected void removeCacheRelationship() {
        DataManager.getInstance().removeFromCacheSet(getDataType(), getCacheKey());
    }

    public void initialize(String createdBy) {

    }

    protected void checkField(String id, String oldContent, String content, String field, boolean create)
            throws DataException {
        if (create) {
            if (content == null || content.length() == 0) {
                throw new DataException(ErrorCode.BAD_REQUEST, "Invalid: " + field);
            }

            String cacheKey = getCacheKeyFromCacheMap(id, content);
            if (cacheKey != null && cacheKey.length() > 0) {
                throw new DataException(ErrorCode.CONFLICT, "Conflict " + field + ": " + content);
            }
        } else {
            if (content != null) {
                if (content.length() == 0) {
                    throw new DataException(ErrorCode.BAD_REQUEST, "Invalid: " + field);
                } else {
                    if (!oldContent.equals(content)) {
                        String cacheKey = getCacheKeyFromCacheMap(id, content);
                        if (cacheKey != null && cacheKey.length() > 0) {
                            throw new DataException(ErrorCode.CONFLICT, "Conflict " + field + ": " + content);
                        }
                    }
                }
            }
        }
    }

    public JsonObject toJsonObject() {
        return Json.jsonEncode(this);
    }

    @Override
    public String toString() {
        return Json.encode(this);
    }

    public String getCacheKey() {
        return getCacheKey(getDataType(), getId());
    }

    public static <M extends ADSData> M getCache(Type type, Object id) {
        String cacheKey = getCacheKey(type, id);
        M data = DataManager.getInstance().getCache(cacheKey);
        if (data == null || data.flag == 1) {
            return null;
        }

        return data;
    }

    public static <M extends ADSData> M getCacheIgnoreLock(Type type, Object id) {
        String cacheKey = getCacheKey(type, id);
        M data = DataManager.getInstance().getCacheIgnoreLock(cacheKey);
        if (data == null || data.flag == 1) {
            return null;
        }
        return data;
    }

    protected static String getCacheKey(Type type, Object id) {
        if (type == null || id == null) {
            return null;
        }

        if (id instanceof String && ((String) id).length() == 0) {
            return null;
        } else if (id instanceof Integer && (Integer) id <= 0) {
            return null;
        }

        return type.getValue() + ":" + id;
    }

    public static <M extends ADSData> M getCache(ADSDbKey.Type type, String id, String key) {
        String cacheKey = DataManager.getInstance().getCacheKey(type, id, key);
        return DataManager.getInstance().getCache(cacheKey);
    }

    public static <M extends ADSData> M getCacheIgnoreLock(ADSDbKey.Type type, String id, String key) {
        String cacheKey = DataManager.getInstance().getCacheKey(type, id, key);
        return DataManager.getInstance().getCacheIgnoreLock(cacheKey);
    }

    protected String getCacheKeyFromCacheMap(String id, String key) {
        return DataManager.getInstance().getCacheKey(getDataType(), id, key);
    }

    protected void addToCacheMap(String id, String key, String cacheKey) {
        DataManager.getInstance().addToCacheMap(getDataType(), id, key, cacheKey);
    }

    protected void removeFromCacheMap(String id, String key) {
        DataManager.getInstance().removeFromCacheMap(getDataType(), id, key);
    }

    public String getCacheKeyFromCacheMap(ADSDbKey.Type toType, String id, String key) {
        String type = data_type + ":" + toType.getValue();
        CacheMap<String, String> cacheMap = DataManager.getInstance().getCacheMap(type, id);
        if (cacheMap != null) {
            return cacheMap.get(key);
        }

        return null;
    }

    protected void addToCacheMap(ADSDbKey.Type toType, String id, String key, String cacheKey) {
        String type = data_type + ":" + toType.getValue();
        CacheMap<String, String> cacheMap = DataManager.getInstance().getCacheMap(type, id);
        if (cacheMap != null) {
            cacheMap.put(key, cacheKey);
        }
    }

    protected void removeFromCacheMap(ADSDbKey.Type toType, String id, String key) {
        String type = data_type + ":" + toType.getValue();
        CacheMap<String, String> cacheMap = DataManager.getInstance().getCacheMap(type, id);
        if (cacheMap != null) {
            cacheMap.remove(key);
        }
    }

    protected MapSet<String> getOrCreateSet(ADSDbKey.Type type) {
        return DataManager.getInstance().getOrCreateSet(data_type, getId(), type.getValue());
    }

    protected SortSet getOrCreateSortSet(ADSDbKey.Type type) {
        return DataManager.getInstance().getOrCreateSortSet(data_type, getId(), type.getValue());
    }

    public <M extends ADSData> M getCacheData() {
        return DataManager.getInstance().getCacheIgnoreLock(getCacheKey());
    }

    public ADSDbKey.Type getDataType() {
        return ADSDbKey.Type.fromString(data_type);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (obj instanceof ADSData) {
            ADSData data = (ADSData) obj;
            if (getId().equals(data.getId())) {
                return true;
            }
        }

        return false;
    }

    public <M extends ADSData> M copy() {
        JsonObject self = toJsonObject();
        if (self != null && data_type != null) {
            try {
                return Json.mapper.readerFor(JCPersistence.getInstance().getModelClass(data_type))
                        .readValue(self.toString());
            } catch (Exception e) {
            }
        }
        return null;
    }

    public void merge(JsonObject data) {
        JsonObject self = toJsonObject();
        if (self != null) {
            String ot = data_type;
            if (ot == null) {
                ot = data.getString(ADSDbKey.TBase.DATA_TYPE);
            }
            // remove all null value
            JsonObject cp = data.copy();
            data.forEach(entry -> {
                if (entry.getValue() == null) {
                    cp.remove(entry.getKey());
                }
            });
            data = cp;
            JsonObject nd = self.mergeIn(data);
            if (nd != null) {
                Json.decodeValue(this, nd.toString());
                data_type = ot;
            } else
                log.error("merge data error: mergeIn");
        } else {
            log.error("merge data error: self == null");
        }
    }

    public void merge(ADSData data) {
        merge(Json.jsonEncodeNonNull(data));
    }

    @Override
    protected Map<String, Object> afterSerializerData(Map<String, Object> data) {
        if (RequestThreadLocal.get(RequestThreadLocal.ThreadDataType.EUserData) != null)
            return ResponseFilter.filterData(this, data);
        else
            return data;
    }

    @Override
    protected String getSerializerName(JsonBeanPropertyWriter writer) {
        JsonField jf = writer.getAnnotation(JsonField.class);
        if (jf != null && jf.responseFilter()
                && RequestThreadLocal.get(RequestThreadLocal.ThreadDataType.EUserData) != null) {
            // ignore this property
            return null;
        }
        if (jf != null && jf.out().length() > 0) {
            return jf.out();
        }
        return writer.getName();
    }

    @Override
    protected Object getSerializerValue(JsonBeanPropertyWriter writer) throws Exception {
        return writer.get(this);
    }

    // public abstract String entityName();
}

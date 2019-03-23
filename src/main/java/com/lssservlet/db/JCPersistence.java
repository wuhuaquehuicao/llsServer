package com.lssservlet.db;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Query;
import javax.persistence.SharedCacheMode;
import javax.persistence.Table;
import javax.persistence.ValidationMode;
import javax.persistence.spi.ClassTransformer;
import javax.persistence.spi.PersistenceUnitInfo;
import javax.persistence.spi.PersistenceUnitTransactionType;
import javax.sql.DataSource;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.SQLQuery;
import org.hibernate.jpa.HibernatePersistenceProvider;
import org.hibernate.transform.Transformers;

import com.lssservlet.api.HandleApplication.QueryParams;
import com.lssservlet.api.HandleBase.QueryParams1;
import com.lssservlet.core.Config;
import com.lssservlet.datamodel.ADSData;
import com.lssservlet.datamodel.ADSDbKey;
import com.lssservlet.datamodel.ADSDbKey.CacheType;
import com.lssservlet.exception.ErrorCode;
import com.lssservlet.utils.DataException;
import com.lssservlet.utils.JCIdAnnotation;
import com.lssservlet.utils.JCModel;
import com.lssservlet.utils.Json;
import com.lssservlet.utils.JsonArray;
import com.lssservlet.utils.JsonObject;

import io.github.lukehutch.fastclasspathscanner.FastClasspathScanner;
import io.github.lukehutch.fastclasspathscanner.scanner.ScanResult;

/**
 * Created by Justek
 */

public class JCPersistence {
    protected static final Logger log = LogManager.getLogger(JCPersistence.class);

    @Converter(autoApply = true)
    public static class IntegerConverter implements AttributeConverter<Integer, Integer> {

        @Override
        public Integer convertToDatabaseColumn(Integer attribute) {
            if (attribute == null)
                return 0;
            return attribute;
        }

        @Override
        public Integer convertToEntityAttribute(Integer dbData) {
            if (dbData == null)
                return 0;
            return dbData;
        }
    }

    @Converter(autoApply = true)
    public static class LongConverter implements AttributeConverter<Long, Long> {

        @Override
        public Long convertToDatabaseColumn(Long attribute) {
            if (attribute == null)
                return 0l;
            return attribute;
        }

        @Override
        public Long convertToEntityAttribute(Long dbData) {
            if (dbData == null)
                return 0l;
            return dbData;
        }
    }

    @Converter(autoApply = true)
    public static class ArrayListConverter implements AttributeConverter<List<String>, String> {
        @Override
        public String convertToDatabaseColumn(List<String> attribute) {
            if (attribute != null && attribute.size() > 0) {
                return Json.encodeNonNull(attribute);
            }
            return null;
        }

        @Override
        public List<String> convertToEntityAttribute(String dbData) {
            if (dbData != null && dbData.length() > 0) {
                try {
                    dbData = dbData.trim();
                    if (!dbData.startsWith("[")) {
                        dbData = "[\"" + dbData;
                    }
                    if (!dbData.endsWith("]")) {
                        dbData = dbData + "\"]";
                    }
                    JsonArray result = new JsonArray(dbData);
                    if (result != null && result.size() > 0) {
                        ArrayList<String> list = new ArrayList<>();
                        for (int i = 0; i < result.size(); i++) {
                            list.add(result.getString(i));
                        }
                        return list;
                    }
                } catch (Exception e) {
                    log.warn("load data error: " + dbData, e);
                }
            }
            return new ArrayList<String>();
        }
    }

    @Converter(autoApply = true)
    public static class HashSetConverter implements AttributeConverter<Set<String>, String> {
        @Override
        public String convertToDatabaseColumn(Set<String> attribute) {
            if (attribute != null && attribute.size() > 0) {
                return Json.encodeNonNull(attribute);
            }
            return null;
        }

        @Override
        public Set<String> convertToEntityAttribute(String dbData) {
            if (dbData != null && dbData.length() > 2) {
                try {
                    JsonArray result = new JsonArray(dbData);
                    if (result != null && result.size() > 0) {
                        HashSet<String> list = new HashSet<>();
                        for (int i = 0; i < result.size(); i++) {
                            list.add(result.getString(i));
                        }
                        return list;
                    }
                } catch (Exception e) {
                    log.warn("load data error: " + dbData, e);
                }
            }
            return new HashSet<String>();
        }
    }

    @Converter(autoApply = false)
    public static class MapConverter implements AttributeConverter<Map<String, Object>, String> {
        @Override
        public String convertToDatabaseColumn(Map<String, Object> attribute) {
            if (attribute != null && attribute.size() > 0) {
                return Json.encodeNonNull(attribute);
            }
            return null;
        }

        @Override
        public Map<String, Object> convertToEntityAttribute(String dbData) {
            JsonObject object = new JsonObject(dbData);
            if (object.size() > 0) {
                Map<String, Object> map = new HashMap<>();
                object.forEach(entry -> {
                    String k = entry.getKey();
                    Object v = entry.getValue();
                    map.put(k, v);
                });

                return map;
            }
            return new HashMap<String, Object>();
        }
    }

    @Converter(autoApply = false)
    public static class ArrayListMapConverter
            implements AttributeConverter<List<Map<String, Map<String, ArrayList<Integer>>>>, String> {
        @Override
        public String convertToDatabaseColumn(List<Map<String, Map<String, ArrayList<Integer>>>> attribute) {
            if (attribute != null && attribute.size() > 0) {
                return Json.encodeNonNull(attribute);
            }
            return null;
        }

        @Override
        public List<Map<String, Map<String, ArrayList<Integer>>>> convertToEntityAttribute(String dbData) {
            if (dbData != null && dbData.length() > 0) {
                try {
                    dbData = dbData.trim();
                    if (!dbData.startsWith("[")) {
                        dbData = "[" + dbData;
                    }
                    if (!dbData.endsWith("]")) {
                        dbData = dbData + "]";
                    }
                    JsonArray result = new JsonArray(dbData);
                    if (result != null && result.size() > 0) {
                        List<Map<String, Map<String, ArrayList<Integer>>>> list = new ArrayList<Map<String, Map<String, ArrayList<Integer>>>>();
                        for (int i = 0; i < result.size(); i++) {
                            // list.add(result.getString(i));
                            JsonObject object = result.getJsonObject(i);
                            object.forEach(entry -> {
                                String twId = entry.getKey();
                                Object value = entry.getValue();
                                Map<String, Map<String, ArrayList<Integer>>> map = new HashMap<>();
                                Map<String, ArrayList<Integer>> mapList = new HashMap<>();
                                if (value instanceof JsonObject) {
                                    JsonObject data = (JsonObject) value;
                                    data.forEach(ent -> {
                                        String key = ent.getKey();
                                        Object val = ent.getValue();
                                        ArrayList<Integer> dataList = new ArrayList<>();
                                        if (val instanceof JsonArray) {
                                            JsonArray values = (JsonArray) val;
                                            for (int index = 0; index < values.size(); index++) {
                                                Integer ret = values.getInteger(index);
                                                if (ret == null) {
                                                    ret = -1;
                                                }
                                                dataList.add(ret);
                                            }
                                        }
                                        mapList.put(key, dataList);
                                    });
                                }
                                map.put(twId, mapList);
                                list.add(map);
                            });
                        }
                        return list;
                    }
                } catch (Exception e) {
                    log.warn("load data error: " + dbData, e);
                }
            }
            return new ArrayList<>();
        }
    }

    @Converter(autoApply = false)
    public static class ArrayListListConverter implements AttributeConverter<List<ArrayList<Integer>>, String> {
        @Override
        public String convertToDatabaseColumn(List<ArrayList<Integer>> attribute) {
            if (attribute != null && attribute.size() > 0) {
                return Json.encodeNonNull(attribute);
            }
            return null;
        }

        @Override
        public List<ArrayList<Integer>> convertToEntityAttribute(String dbData) {
            if (dbData != null && dbData.length() > 0) {
                try {
                    dbData = dbData.trim();
                    if (!dbData.startsWith("[")) {
                        dbData = "[" + dbData;
                    }
                    if (!dbData.endsWith("]")) {
                        dbData = dbData + "]";
                    }
                    JsonArray result = new JsonArray(dbData);
                    if (result != null && result.size() > 0) {
                        List<ArrayList<Integer>> list = new ArrayList<>();
                        for (int i = 0; i < result.size(); i++) {
                            // list.add(result.getString(i));
                            JsonArray object = result.getJsonArray(i);
                            if (object != null) {
                                ArrayList<Integer> dataList = new ArrayList<>();
                                for (int index = 0; index < object.size(); index++) {
                                    Integer value = object.getInteger(index);
                                    if (value == null) {
                                        value = -1;
                                    }
                                    dataList.add(value);
                                }
                                list.add(dataList);
                            }
                        }
                        return list;
                    }
                } catch (Exception e) {
                    log.warn("load data error: " + dbData, e);
                }
            }
            return new ArrayList<>();
        }
    }

    private static PersistenceUnitInfo archiverPersistenceUnitInfo(boolean replica) {
        return new PersistenceUnitInfo() {
            @Override
            public String getPersistenceUnitName() {
                return "lssservlet";
            }

            @Override
            public String getPersistenceProviderClassName() {
                return HibernatePersistenceProvider.class.getName();
            }

            @Override
            public PersistenceUnitTransactionType getTransactionType() {
                return PersistenceUnitTransactionType.RESOURCE_LOCAL;
            }

            @Override
            public DataSource getJtaDataSource() {
                if (replica)
                    return Config.getInstance().getReplicaDataSource();
                else
                    return Config.getInstance().getDataSource();
            }

            @Override
            public DataSource getNonJtaDataSource() {
                return getJtaDataSource();
            }

            @Override
            public List<String> getMappingFileNames() {
                return Collections.emptyList();
            }

            @Override
            public List<URL> getJarFileUrls() {
                try {
                    return Collections.list(this.getClass().getClassLoader().getResources(""));
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            }

            @Override
            public URL getPersistenceUnitRootUrl() {
                return null;
            }

            @Override
            public List<String> getManagedClassNames() {
                ScanResult scanResult = new FastClasspathScanner("com.lssservlet.datamodel").scan();
                List<String> pathAnnotated = scanResult.getNamesOfClassesWithAnnotation(javax.persistence.Entity.class);
                pathAnnotated.add(JCPersistence.HashSetConverter.class.getName());
                pathAnnotated.add(JCPersistence.IntegerConverter.class.getName());
                pathAnnotated.add(JCPersistence.LongConverter.class.getName());
                pathAnnotated.add(JCPersistence.MapConverter.class.getName());
                return pathAnnotated;
            }

            @Override
            public boolean excludeUnlistedClasses() {
                return false;
            }

            @Override
            public SharedCacheMode getSharedCacheMode() {
                return null;
            }

            @Override
            public ValidationMode getValidationMode() {
                return null;
            }

            @Override
            public Properties getProperties() {
                Properties p = new Properties();
                p.setProperty("hibernate.dialect", "org.hibernate.dialect.MySQL5InnoDBDialect");
                p.setProperty("hibernate.hbm2ddl.auto", "update");
                p.setProperty("hibernate.show_sql", "false");
                // p.setProperty("hibernate.connection.driver_class","com.mysql.cj.jdbc.Driver");
                // p.setProperty("hibernate.connection.url","jdbc:mysql://localhost:8086/pos2?tinyInt1isBit=false&amp;useUnicode=true&amp;characterEncoding=utf8&amp;useSSL=false");
                // p.setProperty("hibernate.connection.username","root");
                // p.setProperty("hibernate.connection.password","13");
                return p;
            }

            @Override
            public String getPersistenceXMLSchemaVersion() {
                return null;
            }

            @Override
            public ClassLoader getClassLoader() {
                return null;
            }

            @Override
            public void addTransformer(ClassTransformer transformer) {

            }

            @Override
            public ClassLoader getNewTempClassLoader() {
                return null;
            }
        };
    }

    private class JCModelInfo {
        public String table;
        public ADSDbKey.Type type;
        public Class<?> entityClass;
        public String idType = "";
    }

    private static JCPersistence _shareInstance = null;
    private EntityManagerFactory _master_EntityManagerFactory = null;
    private EntityManagerFactory _replica_EntityManagerFactory = null;
    private ConcurrentHashMap<String, JCModelInfo> _modelInfoList = new ConcurrentHashMap<String, JCModelInfo>();

    public static JCPersistence getInstance() {
        if (_shareInstance == null) {
            synchronized (JCPersistence.class) {
                if (_shareInstance == null) {
                    _shareInstance = new JCPersistence();
                }
            }
        }
        return _shareInstance;
    }

    private JCPersistence() {
        _master_EntityManagerFactory = new HibernatePersistenceProvider()
                .createContainerEntityManagerFactory(archiverPersistenceUnitInfo(false), null);
        _replica_EntityManagerFactory = new HibernatePersistenceProvider()
                .createContainerEntityManagerFactory(archiverPersistenceUnitInfo(true), null);
        ScanResult scanResult = new FastClasspathScanner("com.lssservlet.datamodel").scan();
        List<String> pathAnnotated = scanResult.getNamesOfClassesWithAnnotation(JCModel.class);
        for (String cn : pathAnnotated) {
            try {
                Class<?> entityClass = Class.forName(cn);

                JCModelInfo info = new JCModelInfo();
                info.entityClass = entityClass;

                JCModel[] models = entityClass.getAnnotationsByType(JCModel.class);
                if (models != null && models.length > 0) {
                    JCModel model = models[0];
                    if (_modelInfoList.get(model.type().getValue()) != null) {
                        log.error("duplication type name for JCModel:" + cn);
                        continue;
                    }
                    info.type = model.type();
                }

                Table[] tables = entityClass.getAnnotationsByType(Table.class);
                if (tables != null && tables.length > 0) {
                    info.table = tables[0].name();
                }

                JCIdAnnotation[] idAnnotations = entityClass.getAnnotationsByType(JCIdAnnotation.class);
                if (idAnnotations != null && idAnnotations.length > 0) {
                    info.idType = idAnnotations[0].idType();
                }

                _modelInfoList.put(info.type.getValue(), info);
            } catch (Exception e) {
            }
        }
    }

    public void close() {
        _master_EntityManagerFactory.close();
        _replica_EntityManagerFactory.close();
    }

    public List<ADSDbKey.Type> getModels(CacheType cacheType) {
        ArrayList<ADSDbKey.Type> models = new ArrayList<ADSDbKey.Type>();
        for (JCModelInfo t : _modelInfoList.values()) {
            if (t.type.getCacheType() == cacheType) {
                models.add(t.type);
            }
        }
        return models;
    }

    public List<ADSDbKey.Type> getDatabaseChangedModels() {
        ArrayList<ADSDbKey.Type> models = new ArrayList<ADSDbKey.Type>();
        for (JCModelInfo t : _modelInfoList.values()) {
            if (t.type.checkDatabaseChanged())
                models.add(t.type);
        }
        return models;
    }

    @SuppressWarnings("unchecked")
    public <M extends ADSData> M find(String cacheKey) {
        if (cacheKey == null || cacheKey.length() == 0) {
            return null;
        }
        EntityManager entityManager = _replica_EntityManagerFactory.createEntityManager();
        if (entityManager != null) {
            try {
                String[] ks = cacheKey.split(":");
                if (ks != null && ks.length == 2) {
                    JCModelInfo info = _modelInfoList.get(ks[0]);
                    if (info != null) {
                        Object id = ks[1];
                        if (info.idType.equals("int")) {
                            id = Integer.parseInt(ks[1]);
                        }

                        return entityManager.find((Class<M>) info.entityClass, id);
                    } else {
                        log.warn("invalid mode info: " + cacheKey);
                    }
                }
                return null;
            } catch (Exception e) {
                log.error("find key error:" + cacheKey, e);
            } finally {
                entityManager.close();
            }
        }
        return null;
    }

    public Class<?> getModelClass(String model) {
        JCModelInfo info = _modelInfoList.get(model);
        if (info != null)
            return info.entityClass;
        return null;
    }

    public String getModelTable(String model) {
        JCModelInfo info = _modelInfoList.get(model);
        if (info != null)
            return info.table;
        return null;
    }

    @SuppressWarnings("unchecked")
    public List<ADSData> loadModel(ADSDbKey.Type type) {
        EntityManager entityManager = _replica_EntityManagerFactory.createEntityManager();
        if (entityManager != null) {
            try {
                JCModelInfo info = _modelInfoList.get(type.getValue());
                String sql_statement = "SELECT * FROM " + info.table + " WHERE flag=0";
                return entityManager.createNativeQuery(sql_statement, info.entityClass).getResultList();
            } catch (Exception e) {
                log.error("loadTable error, model:" + type.getValue(), e);
            } finally {
                entityManager.close();
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public <M extends ADSData> List<M> loadModel(ADSDbKey.Type tableType, String condition) {
        List<M> dataList = null;
        EntityManager entityManager = _replica_EntityManagerFactory.createEntityManager();
        if (entityManager != null) {
            try {
                JCModelInfo info = _modelInfoList.get(tableType.getValue());
                String queryCondition = "";
                if (condition != null && condition.length() > 0) {
                    queryCondition += " WHERE " + condition;
                }
                String execute_statement = "SELECT * FROM " + info.table + queryCondition;
                dataList = entityManager.createNativeQuery(execute_statement, info.entityClass).getResultList();
            } catch (Exception e) {
                log.error("loadTable error, model:" + tableType.getValue(), e);
            } finally {
                entityManager.close();
            }
        }
        return dataList;
    }

    @SuppressWarnings({ "deprecation" })
    public List<?> query(ADSDbKey.Type type, QueryParams params) throws DataException {
        List<?> dataList = null;
        EntityManager entityManager = _replica_EntityManagerFactory.createEntityManager();
        if (entityManager != null) {
            try {
                JCModelInfo info = _modelInfoList.get(type.getValue());

                StringBuilder builder = new StringBuilder("SELECT ");
                ArrayList<String> columns = params.columns;
                if (columns == null) {
                    throw new DataException(ErrorCode.INTERNAL_SERVER_ERROR, "Invalid columns");
                }
                for (int index = 0; index < columns.size(); index++) {
                    builder.append(columns.get(index));
                    if (index < columns.size() - 1) {
                        builder.append(", ");
                    }
                }
                builder.append(" FROM " + info.table);

                ArrayList<String> clauses = params.clauses;
                if (clauses != null && clauses.size() > 0) {
                    builder.append(" WHERE ");
                    for (int index = 0; index < clauses.size(); index++) {
                        builder.append(clauses.get(index));
                        if (index < clauses.size() - 1) {
                            builder.append(" AND ");
                        }
                    }
                }

                ArrayList<String> orders = params.orders;
                if (orders != null && orders.size() > 0) {
                    builder.append(" ORDER BY ");
                    for (int index = 0; index < orders.size(); index++) {
                        builder.append(orders.get(index));
                        if (index < orders.size() - 1) {
                            builder.append(", ");
                        }
                    }
                }

                if (params.limit != null) {
                    builder.append(" LIMIT " + params.limit);
                }
                builder.append(";");
                Query query = entityManager.createNativeQuery(builder.toString());
                query.unwrap(SQLQuery.class).setResultTransformer(Transformers.ALIAS_TO_ENTITY_MAP);
                dataList = query.getResultList();
            } catch (Exception e) {
                throw new DataException(ErrorCode.INTERNAL_SERVER_ERROR, e.getMessage());
            } finally {
                entityManager.close();
            }
        }
        return dataList;
    }

    @SuppressWarnings({ "deprecation" })
    public List<?> query(String table, QueryParams1 params) throws DataException {
        List<?> dataList = null;
        EntityManager entityManager = _replica_EntityManagerFactory.createEntityManager();
        if (entityManager != null) {
            try {
                StringBuilder builder = new StringBuilder("SELECT * ");
                builder.append(" FROM " + table);

                ArrayList<String> clauses = params.clauses;
                if (clauses != null && clauses.size() > 0) {
                    builder.append(" WHERE ");
                    for (int index = 0; index < clauses.size(); index++) {
                        builder.append(clauses.get(index));
                        if (index < clauses.size() - 1) {
                            builder.append(" AND ");
                        }
                    }
                }

                ArrayList<String> groups = params.groups;
                if (groups != null && groups.size() > 0) {
                    builder.append(" GROUP BY ");
                    for (int index = 0; index < groups.size(); index++) {
                        builder.append(groups.get(index));
                        if (index < groups.size() - 1) {
                            builder.append(", ");
                        }
                    }
                }

                ArrayList<String> orders = params.orders;
                if (orders != null && orders.size() > 0) {
                    builder.append(" ORDER BY ");
                    for (int index = 0; index < orders.size(); index++) {
                        builder.append(orders.get(index));
                        if (index < orders.size() - 1) {
                            builder.append(", ");
                        }
                    }
                }

                if (params.limit != null) {
                    builder.append(" LIMIT " + params.limit);
                }
                builder.append(";");
                log.debug(builder.toString());
                Query query = entityManager.createNativeQuery(builder.toString());
                query.unwrap(SQLQuery.class).setResultTransformer(Transformers.ALIAS_TO_ENTITY_MAP);
                dataList = query.getResultList();
            } catch (Exception e) {
                throw new DataException(ErrorCode.INTERNAL_SERVER_ERROR, e.getMessage());
            } finally {
                entityManager.close();
            }
        }
        return dataList;
    }

    public List<?> query(ADSDbKey.Type type, QueryParams1 params) throws DataException {
        List<?> dataList = null;
        EntityManager entityManager = _replica_EntityManagerFactory.createEntityManager();
        if (entityManager != null) {
            try {
                JCModelInfo info = _modelInfoList.get(type.getValue());
                StringBuilder builder = new StringBuilder("SELECT *");
                builder.append(" FROM " + info.table);

                ArrayList<String> clauses = params.clauses;
                if (clauses != null && clauses.size() > 0) {
                    builder.append(" WHERE ");
                    for (int index = 0; index < clauses.size(); index++) {
                        builder.append(clauses.get(index));
                        if (index < clauses.size() - 1) {
                            if (params.or)
                                builder.append(" OR ");
                            else
                                builder.append(" AND ");
                        }
                    }
                }

                ArrayList<String> orders = params.orders;
                if (orders != null && orders.size() > 0) {
                    builder.append(" ORDER BY ");
                    for (int index = 0; index < orders.size(); index++) {
                        builder.append(orders.get(index));
                        if (params.desc)
                            builder.append(" DESC ");
                        if (index < orders.size() - 1) {
                            builder.append(", ");
                        }
                    }
                }

                if (params.limit != null) {
                    builder.append(" LIMIT " + params.limit);
                }

                if (params.offset != null) {
                    builder.append(" OFFSET " + params.offset);
                }
                builder.append(";");
                Query query = entityManager.createNativeQuery(builder.toString(), info.entityClass);
                dataList = query.getResultList();
            } catch (Exception e) {
                throw new DataException(ErrorCode.INTERNAL_SERVER_ERROR, e.getMessage());
            } finally {
                entityManager.close();
            }
        }
        return dataList;
    }

    @SuppressWarnings({ "deprecation" })
    public List<?> query(String sql_statement) throws DataException {
        List<?> dataList = null;
        EntityManager entityManager = _replica_EntityManagerFactory.createEntityManager();
        if (entityManager != null) {
            try {
                Query query = entityManager.createNativeQuery(sql_statement);
                query.unwrap(SQLQuery.class).setResultTransformer(Transformers.ALIAS_TO_ENTITY_MAP);
                dataList = query.getResultList();
            } catch (Exception e) {
                throw new DataException(ErrorCode.INTERNAL_SERVER_ERROR, e.getMessage());
            } finally {
                entityManager.close();
            }
        }
        return dataList;
    }

    public Boolean update(ADSData data) {
        EntityManager entityManager = _master_EntityManagerFactory.createEntityManager();
        if (entityManager != null) {
            try {
                entityManager.getTransaction().begin();
                data = entityManager.merge(data);
                entityManager.getTransaction().commit();
                return true;
            } catch (Exception e) {
                log.error("update error:\n" + Json.encodePretty(data), e);
            } finally {
                entityManager.close();
            }
        }
        return false;
    }

    public Boolean delete(ADSData data) {
        EntityManager entityManager = _master_EntityManagerFactory.createEntityManager();
        if (entityManager != null) {
            try {
                ADSData entity = entityManager.find(data.getClass(), data.getId());
                if (entity != null) {
                    entityManager.getTransaction().begin();
                    entityManager.remove(entity); // update ID
                    entityManager.getTransaction().commit();
                }
                return true;
            } catch (Exception e) {
                log.error("remove error:\n" + Json.encodePretty(data), e);
            } finally {
                log.info("entity manager close");
                entityManager.close();
            }
        }

        return false;
    }

    public Boolean persist(ADSData data) {
        EntityManager entityManager = _master_EntityManagerFactory.createEntityManager();
        if (entityManager != null) {
            try {
                entityManager.getTransaction().begin();
                entityManager.persist(data); // update ID
                entityManager.getTransaction().commit();
                return true;
            } catch (Exception e) {
                log.error("insert error:\n" + Json.encodePretty(data), e);
            } finally {
                log.info("entity manager close");
                entityManager.close();
            }
        }
        return false;
    }

    public interface TransactionHandler {
        public void beginExecuteTranscation();

        public void executeTransaction(EntityManager em);

        public void transactionFinished();
    }

    public void executeTransaction(TransactionHandler... args) throws DataException {
        boolean addToDatabase = false;
        EntityManager em = _master_EntityManagerFactory.createEntityManager();
        if (em != null) {
            try {
                for (TransactionHandler tran : args) {
                    if (tran != null) {
                        tran.beginExecuteTranscation();
                    }
                }

                em.getTransaction().begin();
                for (TransactionHandler tran : args) {
                    if (tran != null) {
                        tran.executeTransaction(em);
                    }
                }
                em.getTransaction().commit();

                addToDatabase = true;
            } catch (Exception e) {
                em.getTransaction().rollback();
                throw new DataException(ErrorCode.INTERNAL_SERVER_ERROR, e.getMessage());
            } finally {
                em.close();

                if (addToDatabase) {
                    for (TransactionHandler tran : args) {
                        if (tran != null) {
                            tran.transactionFinished();
                        }
                    }
                }
            }
        }
    }

    public <M extends TransactionHandler> void executeTransaction(ArrayList<M> transactionList) throws DataException {
        boolean addToDatabase = false;
        EntityManager em = _master_EntityManagerFactory.createEntityManager();
        if (em != null) {
            try {
                if (transactionList != null && !transactionList.isEmpty()) {
                    for (TransactionHandler tran : transactionList) {
                        if (tran != null) {
                            tran.beginExecuteTranscation();
                        }
                    }
                }

                em.getTransaction().begin();

                if (transactionList != null && !transactionList.isEmpty()) {
                    for (TransactionHandler tran : transactionList) {
                        if (tran != null) {
                            tran.executeTransaction(em);
                        }
                    }
                }

                em.getTransaction().commit();

                addToDatabase = true;
            } catch (Exception e) {
                em.getTransaction().rollback();
                throw new DataException(ErrorCode.INTERNAL_SERVER_ERROR, e.getMessage());
            } finally {
                em.close();

                if (addToDatabase) {
                    if (transactionList != null && !transactionList.isEmpty()) {
                        for (TransactionHandler tran : transactionList) {
                            if (tran != null) {
                                tran.transactionFinished();
                            }
                        }
                    }
                }
            }
        }
    }
}

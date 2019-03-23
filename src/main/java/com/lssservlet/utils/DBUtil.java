package com.lssservlet.utils;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.TimeZone;

import javax.sql.DataSource;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.lssservlet.exception.ErrorCode;

public class DBUtil {
    public static long delaytimer = 400;
    public long mgetConnectDelay = 0;
    public long mgetInsertDelay = 0;
    public long mgetUpdateDelay = 0;
    public long mgetQueryDelay = 0;
    private Logger log = null;
    private DataSource mDataSource = null;
    private SimpleDateFormat mSimpleDateFormat = null;

    public DBUtil(DataSource aDataSource, Boolean enableLog) {
        mDataSource = aDataSource;
        mSimpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        mSimpleDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        if (enableLog)
            log = LogManager.getLogger(DBUtil.class);
    }

    public void onSqlError(SQLException e, String sql) {
        if (log != null)
            log.error(sql, e);
    }

    public void onSqlWarn(String message) {
        if (log != null)
            log.warn(message);
    }

    private void close(Connection connection) {
        try {
            if (connection != null)
                connection.close();
        } catch (SQLException e) {
            onSqlError(e, "DBUtil close");
        }
    }

    public String getDelayInfo() {
        return String.format("con=%d insert=%d update=%d query=%d", mgetConnectDelay, mgetInsertDelay, mgetUpdateDelay,
                mgetQueryDelay);
    }

    private Connection getConnection() {
        if (mDataSource != null) {
            long startTime = System.currentTimeMillis();
            Connection connection = null;
            synchronized (mDataSource) {
                try {
                    connection = mDataSource.getConnection();
                } catch (SQLException e) {
                    this.onSqlError(e, "getConnection");
                    close(connection);
                    return null;
                }
            }
            if (connection != null) {
                Statement stmt = null;
                try {
                    connection.setAutoCommit(true);
                    stmt = connection.createStatement();
                    stmt.executeUpdate("SET CHARACTER SET utf8mb4");
                    stmt.executeUpdate("SET NAMES utf8mb4");
                } catch (SQLException e) {
                    this.onSqlError(e, "getConnection");
                    close(connection);
                } catch (Exception e) {
                    e.printStackTrace();
                    close(connection);
                } finally {
                    if (stmt != null) {
                        try {
                            stmt.close();
                        } catch (SQLException e) {
                        }
                    }
                }
                mgetConnectDelay += (System.currentTimeMillis() - startTime);
                return connection;
            }
        }
        return null;
    }

    public Object getValueForUpdate(String key, Object value) {
        if (value instanceof JsonArray || value instanceof JsonObject) {
            value = value.toString();
        } else if (key.equals("created_at") || key.equals("updated_at")) {
            try {
                return mSimpleDateFormat.format(new Date((Long) value));
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
        }
        return value;
    }

    public long insert(String table, JsonObject data, JsonObject dupUpdate) throws DataException, SQLException {
        if (data == null || data.size() == 0) {
            return 0l;
        }
        long startTime = System.currentTimeMillis();
        StringBuilder sb1 = new StringBuilder();
        StringBuilder sb2 = new StringBuilder();
        sb1.append("insert into " + table + "(");
        sb2.append(" values(");

        JsonArray params = new JsonArray();
        Iterator<Entry<String, Object>> it = data.iterator();
        while (it.hasNext()) {
            Entry<String, Object> item = it.next();
            String k = item.getKey();
            if (item.getValue() != null) {
                sb1.append(k + ",");
                sb2.append("?,");
                params.add(getValueForUpdate(k, item.getValue()));
            }
        }
        StringBuilder sb = new StringBuilder(
                sb1.substring(0, sb1.length() - 1) + ")" + sb2.substring(0, sb2.length() - 1) + ")");
        if (dupUpdate != null && dupUpdate.size() > 0) {
            sb.append(" ON DUPLICATE KEY UPDATE ");
            it = dupUpdate.iterator();
            while (it.hasNext()) {
                Entry<String, Object> item = it.next();
                String k = item.getKey();
                if (item.getValue() != null) {
                    sb.append(k + " = ? , ");
                    params.add(getValueForUpdate(k, item.getValue()));
                }
            }
            sb.delete(sb.length() - 2, sb.length());// remove ", "
        }
        String sql = sb.toString();
        long autoInckey = -1l;
        java.sql.PreparedStatement ps = null;
        Connection connection = getConnection();
        if (connection == null) {
            mgetInsertDelay += (System.currentTimeMillis() - startTime);
            throw new DataException(ErrorCode.INTERNAL_SERVER_ERROR, "no connection");
        }

        ResultSet rs = null;
        try {
            ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            if (params != null) {
                for (int i = 0; i < params.size(); i++) {
                    ps.setObject(i + 1, params.getValue(i));
                }
            }
            ps.executeUpdate();
            rs = ps.getGeneratedKeys();
            if (rs.next()) {
                autoInckey = rs.getLong(1);
            } else {

            }

        } catch (SQLException e) {
            mgetInsertDelay += (System.currentTimeMillis() - startTime);
            throw new SQLException(table + ":" + e.getMessage(), e.getSQLState(), e.getErrorCode());
        } finally {
            if (rs != null) {
                rs.close();
            }
            if (ps != null) {
                ps.close();
            }
            close(connection);
        }

        mgetInsertDelay += (System.currentTimeMillis() - startTime);
        slowQueryLog(sql, System.currentTimeMillis() - startTime);
        return autoInckey;
    }

    public long update(String table, JsonObject data, JsonObject condition, int limit)
            throws DataException, SQLException {
        if (data == null || data.size() == 0) {
            return 0l;
        }
        long startTime = System.currentTimeMillis();
        StringBuilder sb = new StringBuilder();
        sb.append("update " + table + " set ");
        JsonArray params = new JsonArray();
        Iterator<Entry<String, Object>> it = data.iterator();
        while (it.hasNext()) {
            Entry<String, Object> item = it.next();
            String k = item.getKey();
            if (item.getValue() != null) {
                params.add(getValueForUpdate(k, item.getValue()));
                sb.append(k + " = ?,");
            }
        }
        sb.delete(sb.length() - 1, sb.length()).toString();// remove ","
        if (condition != null && condition.size() > 0) {
            sb.append(" WHERE ");
            it = condition.iterator();
            while (it.hasNext()) {
                Entry<String, Object> item = it.next();
                String k = item.getKey();
                sb.append(k + " = ? AND ");
                params.add(getValueForUpdate(k, item.getValue()));
            }
            sb.delete(sb.length() - 4, sb.length());// remove "AND "
        }
        if (limit > 0) {
            sb.append(" LIMIT " + limit);
        }
        String sql = sb.toString();

        java.sql.PreparedStatement ps = null;
        int rowCount = 0;
        Connection connection = getConnection();
        if (connection == null) {
            mgetUpdateDelay += (System.currentTimeMillis() - startTime);
            throw new DataException(ErrorCode.INTERNAL_SERVER_ERROR, "no connection");
        }
        try {
            ps = connection.prepareStatement(sql);
            if (params != null) {
                for (int i = 0; i < params.size(); i++) {
                    ps.setObject(i + 1, params.getValue(i));
                }
            }
            rowCount = ps.executeUpdate();
            slowQueryLog(sql, System.currentTimeMillis() - startTime);
        } catch (SQLException e) {
            throw new SQLException(e.getMessage(), e.getSQLState(), e.getErrorCode());
        } finally {
            if (ps != null) {
                ps.close();
            }
            close(connection);
            mgetUpdateDelay += (System.currentTimeMillis() - startTime);
        }
        return rowCount;
    }

    public JsonObject queryRow(String table, JsonObject condition) throws DataException {
        JsonArray result = this.query(table, condition, 1);
        if (result != null && result.size() > 0) {
            try {
                return result.getJsonObject(0);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    public JsonArray query(String table, JsonObject condition, int limit) throws DataException {
        long startTime = System.currentTimeMillis();
        JsonArray result = new JsonArray();
        ResultSet rs = null;
        java.sql.PreparedStatement ps = null;
        Connection connection = getConnection();
        if (connection == null) {
            mgetQueryDelay += (System.currentTimeMillis() - startTime);
            throw new DataException(ErrorCode.INTERNAL_SERVER_ERROR, "no connection");
        }
        String sql = null;
        try {
            JsonArray params = new JsonArray();
            StringBuilder sb = new StringBuilder();
            sb.append("select * from " + table);
            if (condition != null && condition.size() > 0) {
                sb.append(" WHERE ");
                Iterator<Entry<String, Object>> it = condition.iterator();
                while (it.hasNext()) {
                    Entry<String, Object> item = it.next();
                    String k = item.getKey();
                    sb.append(k + " = ? AND ");
                    params.add(getValueForUpdate(k, item.getValue()));
                }
                sb.delete(sb.length() - 4, sb.length());// remove "AND "
            }
            if (limit > 0) {
                sb.append(" LIMIT " + limit);
            }
            sql = sb.toString();
            ps = connection.prepareStatement(sql);
            if (params != null) {
                for (int i = 0; i < params.size(); i++) {
                    ps.setObject(i + 1, params.getValue(i));
                }
            }
            rs = ps.executeQuery();
            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();
            while (rs.next()) {
                JsonObject jsonObj = new JsonObject();
                for (int i = 1; i <= columnCount; i++) {
                    String columnName = metaData.getColumnLabel(i);
                    Object value = rs.getObject(columnName);
                    // if (metaData instanceof com.mysql.cj.jdbc.result.ResultSetMetaData) {
                    // com.mysql.cj.core.result.Field f = ((com.mysql.cj.jdbc.result.ResultSetMetaData) metaData)
                    // .getFields()[i - 1];
                    // if (f.getMysqlType() == com.mysql.cj.core.MysqlType.JSON) {
                    // System.out.println(value.toString());
                    // }
                    // }
                    if (value == null) {
                        jsonObj.putNull(columnName);
                    } else if (value instanceof Boolean) {
                        Boolean bValue = (Boolean) value;
                        jsonObj.put(columnName, bValue ? 1 : 0);
                        throw new DataException(ErrorCode.INTERNAL_SERVER_ERROR, "please set tinyInt1isBit=false");
                    } else if (value instanceof java.sql.Timestamp) {
                        java.sql.Timestamp ts = (java.sql.Timestamp) value;
                        jsonObj.put(columnName, ts.getTime());
                    } else if (value instanceof java.math.BigDecimal) {
                        java.math.BigDecimal bd = (java.math.BigDecimal) value;
                        jsonObj.put(columnName, bd.floatValue());
                    } else {
                        if (value instanceof String) {
                            try {
                                if (((String) value).startsWith("{") && ((String) value).endsWith("}")) {
                                    value = new JsonObject((String) value);
                                } else if (((String) value).startsWith("[") && ((String) value).endsWith("]")) {
                                    value = new JsonArray((String) value);
                                }
                            } catch (Exception e) {

                            }
                        }
                        jsonObj.put(columnName, value);
                    }
                }
                result.add(jsonObj);
            }
            slowQueryLog(sql, System.currentTimeMillis() - startTime);
        } catch (SQLException e) {
            this.onSqlError(e, sql);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (rs != null)
                try {
                    rs.close();
                } catch (SQLException e) {
                    this.onSqlError(e, null);
                }
            if (ps != null)
                try {
                    ps.close();
                } catch (SQLException e) {
                    this.onSqlError(e, null);
                }
            close(connection);
            mgetQueryDelay += (System.currentTimeMillis() - startTime);
        }
        return result;
    }

    private void slowQueryLog(String sql, long timeSpent) {
        if (timeSpent > delaytimer) {
            // log the slow query
            if (sql.length() > 300) {
                onSqlWarn("Time spent: " + timeSpent + " , Query: " + sql.substring(0, 300));
            } else {
                onSqlWarn("Time spent: " + timeSpent + " , Query: " + sql);
            }
        }
    }
}

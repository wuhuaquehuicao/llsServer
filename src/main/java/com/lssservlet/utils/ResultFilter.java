package com.lssservlet.utils;

import java.util.ArrayList;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.lssservlet.api.HandleBase.QueryResultParams;
import com.lssservlet.datamodel.ADSData;

public class ResultFilter {
    protected static final Logger log = LogManager.getLogger(ResultFilter.class);

    static public void clear() {
    }

    static class CompareType {
        String key;
        String value;
        String symbol;
    }

    public static <T extends ADSData> Object filterWithPaging(ArrayList<T> data, String[] keywords, int limit,
            int offset, boolean union) {
        if (limit > 1) {
            QueryResultParams params = new QueryResultParams();
            params.page_index = offset / limit + (offset % limit > 0 ? 1 : 0);
            params.page_count = limit;
            Integer total_count = data.size();
            params.total_page = total_count / limit + (total_count % limit > 0 ? 1 : 0);
            params.total_count = total_count;
            params.elements = filter(data, keywords, limit, offset, union);
            return params;
        } else {
            return filter(data, keywords, limit, offset, union);
        }
    }

    // keywords [testMode>=1, note<=a]
    // filter=status==open|accepted|paid&filter=updated_time%3E1488525687658
    // filter=key==asc|des
    public static <T extends ADSData> ArrayList<T> filter(ArrayList<T> data, String[] keywords, int limit, int offset,
            boolean union) {
        if (data == null || data.size() == 0)
            return data;
        ArrayList<T> result = new ArrayList<T>();
        ArrayList<CompareType> filterList = new ArrayList<CompareType>();
        if (keywords != null && keywords.length > 0) {
            for (int i = 0; i < keywords.length; i++) {
                String symbol = null;
                if (keywords[i].contains("==")) {
                    symbol = "==";
                } else if (keywords[i].contains("!=")) {
                    symbol = "!=";
                } else if (keywords[i].contains(">=")) {
                    symbol = ">=";
                } else if (keywords[i].contains("<=")) {
                    symbol = "<=";
                } else if (keywords[i].contains(">")) {
                    symbol = ">";
                } else if (keywords[i].contains("<")) {
                    symbol = "<";
                }
                if (symbol != null) {
                    String[] kv = keywords[i].split(symbol);
                    if (kv != null && kv.length == 2) {
                        String k = kv[0];
                        String v = kv[1];
                        CompareType t = new CompareType();
                        t.symbol = symbol;
                        t.value = v;
                        t.key = k;
                        filterList.add(t);
                    }
                }
            }
        }
        ArrayList<T> list = data;
        if (!union) {
            log.debug("filter: intersection");
            for (CompareType t : filterList) {
                list = filter(t.key, t, list);
                log.debug("filter: {} {} {}", t.key, t.symbol, t.value);
            }
        } else {
            log.debug("filter: unionsection");
            ArrayList<T> unionList = new ArrayList<>();
            for (CompareType t : filterList) {
                ArrayList<T> tempList = filter(t.key, t, list);
                log.debug("filter: {} {} {}", t.key, t.symbol, t.value);
                ArrayList<T> toRemove = new ArrayList<>();
                for (T node : tempList) {
                    if (unionList.contains(node))
                        toRemove.add(node);
                }
                tempList.removeAll(toRemove);
                unionList.addAll(tempList);
            }
            list = unionList;
        }

        if (limit > 0 || offset > 0 && list != null) {
            for (int i = offset; i < list.size(); i++) {
                result.add(list.get(i));
                if (limit > 0 && result.size() >= limit)
                    break;
            }
            return result;
        }

        return list;
    }

    static <T extends ADSData> ArrayList<T> filter(String key, CompareType t, ArrayList<T> data) {
        ArrayList<T> result = new ArrayList<T>();
        for (int i = 0; i < data.size(); i++) {
            JsonObject d = data.get(i).toJsonObject();
            if (d.containsKey(key)) {
                if (t.symbol.equals("==")) {
                    //// filter=status==open|accepted|paid&filter=updated_time%3E1488525687658
                    if (t.value.contains("|")) {
                        String[] v = t.value.split("\\|");
                        if (v != null && v.length > 0) {
                            for (int j = 0; j < v.length; j++) {
                                if (equals(d.getValue(key), v[j])) {
                                    result.add(data.get(i));
                                    break;
                                }
                            }
                        }
                    } else {
                        if (equals(d.getValue(key), t.value)) {
                            result.add(data.get(i));
                        }
                    }
                } else if (t.symbol.equals("!=")) {
                    if (!equals(d.getValue(key), t.value)) {
                        result.add(data.get(i));
                    }
                } else if (t.symbol.equals(">=")) {
                    Object o1 = d.getValue(key);
                    if (o1 instanceof Number && o1.getClass() != t.value.getClass()) {
                        if (o1 instanceof Float || o1 instanceof Double) {
                            Double d1 = Double.valueOf(o1.toString());
                            Double d2 = Double.valueOf(t.value);
                            if (d1 >= d2)
                                result.add(data.get(i));
                        } else {
                            Number n1 = (Number) o1;
                            Number n2 = (Number) Long.parseLong(t.value);
                            if (n1.longValue() >= n2.longValue())
                                result.add(data.get(i));
                        }
                    }
                } else if (t.symbol.equals("<=")) {
                    Object o1 = d.getValue(key);
                    if (o1 instanceof Number && o1.getClass() != t.value.getClass()) {
                        if (o1 instanceof Float || o1 instanceof Double) {
                            Double d1 = Double.valueOf(o1.toString());
                            Double d2 = Double.valueOf(t.value);
                            if (d1 <= d2)
                                result.add(data.get(i));
                        } else {
                            Number n1 = (Number) o1;
                            Number n2 = (Number) Long.parseLong(t.value);
                            if (n1.longValue() <= n2.longValue())
                                result.add(data.get(i));
                        }
                    }
                } else if (t.symbol.equals(">")) {
                    Object o1 = d.getValue(key);
                    if (o1 instanceof Number && o1.getClass() != t.value.getClass()) {
                        if (o1 instanceof Float || o1 instanceof Double) {
                            Double d1 = Double.valueOf(o1.toString());
                            Double d2 = Double.valueOf(t.value);
                            if (d1 > d2)
                                result.add(data.get(i));
                        } else {
                            Number n1 = (Number) o1;
                            Number n2 = (Number) Long.parseLong(t.value);
                            if (n1.longValue() > n2.longValue())
                                result.add(data.get(i));
                        }
                    }
                } else if (t.symbol.equals("<")) {
                    Object o1 = d.getValue(key);
                    if (o1 instanceof Number && o1.getClass() != t.value.getClass()) {
                        if (o1 instanceof Float || o1 instanceof Double) {
                            Double d1 = Double.valueOf(o1.toString());
                            Double d2 = Double.valueOf(t.value);
                            if (d1 < d2)
                                result.add(data.get(i));
                        } else {
                            Number n1 = (Number) o1;
                            Number n2 = (Number) Long.parseLong(t.value);
                            if (n1.longValue() < n2.longValue())
                                result.add(data.get(i));
                        }
                    }
                }
            }
        }
        return result;
    }

    static boolean equals(Object o1, String o2) {
        if (o1 == o2)
            return true;
        if (o1 instanceof String) {
            return o1.equals(o2);
        }
        if (o1 instanceof Number) {
            if (o1 instanceof Float || o1 instanceof Double) {
                Double d1 = Double.valueOf(o1.toString());
                Double d2 = Double.valueOf(o2);
                return d1.equals(d2);
            } else {
                Number n1 = (Number) o1;
                Number n2 = (Number) Long.parseLong(o2);
                return n1.longValue() == n2.longValue();
            }
        }
        return false;
    }
}

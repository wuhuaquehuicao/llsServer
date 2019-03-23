package com.lssservlet.utils;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TimeZone;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.lssservlet.core.DataManager;

public class CSVUtil {
    private static HashSet<String> _pVenuesSet = new HashSet<>();
    private static HashMap<String, String> _pVenuesMap = new HashMap<>();
    protected static final Logger log = LogManager.getLogger(CSVUtil.class);

    public static void updateSort() {
        ArrayList<HashMap<String, String>> sorts = parseCSV("/Users/ramon/Downloads/t_sort.csv");
        ArrayList<HashMap<String, String>> categories = parseCSV("/Users/ramon/Downloads/t_category.csv");
        HashMap<String, HashMap<String, String>> cateoryMap = new HashMap<String, HashMap<String, String>>();
        for (int i = 0; i < categories.size(); i++) {
            HashMap<String, String> category = categories.get(i);
            cateoryMap.put(category.get("id"), category);
        }
        for (int i = 0; i < sorts.size(); i++) {
            HashMap<String, String> sort = sorts.get(i);
            String v = sort.get("value");
            JsonArray ids = new JsonArray(v);
            if (ids != null) {

            }
        }
    }

    public static void main(String[] args) throws Exception {
        // dealMerge();
        // generateDeals();
        String file = "/Users/justek/Public/ads/test.csv";
        JsonArray array = new JsonArray();
        JsonObject data1 = new JsonObject("{\"id\":\"0x01\",\"label\":\"data1\",\"time\":\"2018-10-31\"}");
        JsonObject data2 = new JsonObject("{\"id\":\"0x02\",\"label\":\"data2\",\"time\":\"2018-11-01\"}");
        array.add(data1);
        array.add(data2);
        exportDataToCSV(file, array);
    }

    public static String[] updateItem(HashMap<String, String> oldItem, HashMap<String, String> newItem) {
        ArrayList<String> result = new ArrayList<String>();
        result.add(newItem.get("id"));
        result.add(newItem.get("name"));
        result.add(oldItem.get("name"));
        JsonObject data = new JsonObject();
        String new_display_name = newItem.get("display_name");
        String old_display_name = oldItem.get("display_name");
        if (new_display_name == null || new_display_name.length() == 0) {
            if (old_display_name != null && old_display_name.length() > 0) {
                data.put("display_name", old_display_name);
                result.add(old_display_name);
            } else {
                result.add("");
            }
        } else {
            result.add(new_display_name);
        }
        String new_category_ufo = newItem.get("category_ufo");
        String old_category_ufo = oldItem.get("category_ufo");
        if (new_category_ufo == null || new_category_ufo.length() == 0) {
            if (old_category_ufo != null && old_category_ufo.length() > 0) {
                data.put("category_ufo", old_category_ufo);
                result.add(old_category_ufo);
            } else {
                result.add("");
            }
        } else {
            result.add(new_category_ufo);
        }
        String new_image_url = newItem.get("image_url");
        String old_image_url = oldItem.get("image_url");
        if (new_image_url == null || new_image_url.length() == 0) {
            if (old_image_url != null && old_image_url.length() > 0) {
                data.put("image_url", old_image_url);
                result.add(old_image_url);
            } else {
                result.add("");
            }
        } else {
            result.add(new_image_url);
        }
        String new_description = newItem.get("description");
        String old_description = oldItem.get("description");
        if (new_description == null || new_description.length() == 0) {
            if (old_description != null && old_description.length() > 0) {
                data.put("description", old_description);
                result.add(old_description);
            } else {
                result.add("");
            }
        } else {
            result.add(new_description);
        }
        if (data.size() > 0) {
            System.out.println(update("t_item", data, new JsonObject().put("id", newItem.get("id")), 1));
        }
        return result.toArray(new String[result.size()]);
    }

    public static String escapeString(String sql) {
        sql = sql.replaceAll("\\\\", "\\\\\\\\");
        sql = sql.replaceAll("\\n", "\\\\n");
        sql = sql.replaceAll("\\r", "\\\\r");
        sql = sql.replaceAll("\\t", "\\\\t");
        sql = sql.replaceAll("\\00", "\\\\0");
        sql = sql.replaceAll("'", "\\\\'");
        sql = sql.replaceAll("\\\"", "\\\\\"");
        return sql;
    }

    public static String update(String table, JsonObject data, JsonObject condition, int limit) {
        if (data == null || data.size() == 0) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("update " + table + " set ");
        Iterator<Entry<String, Object>> it = data.iterator();
        while (it.hasNext()) {
            Entry<String, Object> item = it.next();
            String k = item.getKey();
            if (item.getValue() != null) {
                sb.append(k + " = '" + escapeString(item.getValue().toString()) + "',");
            }
        }
        sb.delete(sb.length() - 1, sb.length()).toString();// remove ","
        if (condition != null && condition.size() > 0) {
            sb.append(" WHERE ");
            it = condition.iterator();
            while (it.hasNext()) {
                Entry<String, Object> item = it.next();
                String k = item.getKey();
                sb.append(k + " = '" + escapeString(item.getValue().toString()) + "' AND ");
            }
            sb.delete(sb.length() - 4, sb.length());// remove "AND "
        }
        if (limit > 0) {
            sb.append(" LIMIT " + limit);
        }
        sb.append(";");
        return sb.toString();
    }

    public static void checkItemList(HashMap<String, String> timeWindowMap, ArrayList<HashMap<String, String>> items) {
        for (int i = 0; i < items.size(); i++) {
            HashMap<String, String> item = items.get(i);
            String script = item.get("script");
            JsonObject s = new JsonObject(script);
            if (s != null && s.size() > 0) {
                System.out.println(item.get("id"));
                s.forEach(e -> {
                    String tw = e.getKey();
                    String v = e.getValue().toString();
                    String time = timeWindowMap.get(tw);
                    Boolean hidden = false;
                    if (v.contains("true")) {
                        hidden = true;
                    }
                    if (v.contains("false")) {
                        hidden = false;
                    }
                    String op = item.get("price");
                    String np = "";
                    int p1 = v.indexOf("setPrice(");
                    if (p1 > 0) {
                        int p2 = v.indexOf(")", p1);
                        if (p2 > 0) {
                            np = v.substring(p1 + 9, p2);
                        }
                    }
                    JsonArray ta = new JsonArray(time);
                    if (ta != null && ta.size() == 2) {
                        if (ta.getString(1).equals("0")) {
                            ta.remove(1);
                            ta.add("2400");
                        }
                        if (Long.parseLong(ta.getString(1)) < Long.parseLong(ta.getString(0))) {
                            System.out.println("invalid item time");
                        }
                    }
                    if (!op.equals(np) && np.length() > 0 && !hidden)
                        System.out.println(
                                "\t : " + ta.toString() + "->" + "hidden:" + hidden + ", price:" + op + " ->" + np);
                    else
                        System.out.println("\t : " + ta.toString() + "->" + "hidden:" + hidden);
                });
            }
        }
    }

    public static ArrayList<HashMap<String, String>> parseCSV(String fileName) {
        ArrayList<HashMap<String, String>> result = new ArrayList<HashMap<String, String>>();
        try {
            Reader in = new FileReader(fileName);
            in.skip(1);
            Iterable<CSVRecord> records = CSVFormat.RFC4180.withFirstRecordAsHeader().parse(in);
            for (CSVRecord record : records) {
                try {
                    Map<String, String> mp = record.toMap();
                    result.add((HashMap<String, String>) mp);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    private enum ActiveType {
        EActive, EExpired, EDeleted
    }

    private enum DealType {
        EEvent, EAmount, EPersentage
    }

    // private static void dealMerge() {
    // ArrayList<UFODeal> deals = new ArrayList<>();
    // int deletedCount = 0;
    // int totalCount = 0;
    // initPVenuesSet();
    // initPVenuesMap();
    // AlphaId _alphaId = AlphaId.Default();
    //
    // ArrayList<HashMap<String, String>> hDeals = parseCSV(
    // "/Users/justek/Public/ufo/Hiccup_Transfer/download/Deals_1-30-2.csv");
    // for (int i = 0; i < hDeals.size(); i++) {
    // HashMap<String, String> hDeal = hDeals.get(i);
    // if (isValidMerchant(hDeal.get("VenueId"))) {
    // totalCount++;
    // UFODeal deal = new UFODeal();
    // deal.id = UFODbKey.Provider.EUFO.getPrefix() + _alphaId.getEncodeId(1000);
    // ;// DataManager.getInstance().createEncodeId();// UFODbKey.Provider.EUFO.getPrefix() +
    // // hDeal.get("d");
    // if (getDealStatus(hDeal) == ActiveType.EDeleted) {
    // deal.flag = 1;
    // deletedCount++;
    // } else {
    // deal.flag = 0;
    // }
    // deal.created_time = getLongTime(hDeal.get("CreatedDate"));
    // deal.merchant_id = _pVenuesMap.get(hDeal.get("VenueId"));
    // deal.title = hDeal.get("Title");
    // deal.description = hDeal.get("Subtitle");
    // deal.image = hDeal.get("ImageUrl");
    // deal.start_time = getLongTime(hDeal.get("StartDate"));
    // deal.end_time = getLongTime(hDeal.get("EndDate"));
    //
    // if (!hDeal.get("MaxRedemptionsCount").isEmpty())
    // deal.max_count = Long.parseLong(hDeal.get("MaxRedemptionsCount"));
    // if (!hDeal.get("MaxRedemptionsPerUser").isEmpty())
    // deal.max_count_per_user = Long.parseLong(hDeal.get("MaxRedemptionsPerUser"));
    // // if (!hDeal.get("RedemptionsCount").isEmpty())
    // // deal.claim_count = Long.parseLong(hDeal.get("RedemptionsCount"));
    //
    // HashMap<String, Object> value = new HashMap<>();
    // DealType dealType = getDealType(hDeal);
    // value.put("type", intTypeFromDealType(dealType));
    // if (dealType == DealType.EPersentage)
    // value.put("percentage", Long.parseLong(hDeal.get("PercentageValue")));
    // if (!hDeal.get("MaxValue").isEmpty())
    // value.put("max_amount", Long.parseLong(hDeal.get("MaxValue")) * 100);
    // deal.value = value;
    // if (!hDeal.get("MinimumSpend").isEmpty())
    // deal.mini_spend = Long.parseLong(hDeal.get("MinimumSpend")) * 100;
    // if (dealType == DealType.EEvent && !deal.isExpired() && deal.flag == 0) {
    // deals.add(deal);
    // }
    //
    // }
    // }
    //
    // File file = new File("/Users/justek/Public/ufo/Hiccup_Transfer/download/deal_merge_sql.txt");
    // // 文件不存在时候，主动穿件文件。
    // if (!file.exists()) {
    // try {
    // file.createNewFile();
    // } catch (Exception e) {
    // // TODO: handle exception
    // }
    // }
    // try {
    // FileWriter fw = new FileWriter(file, false);
    // BufferedWriter bw = new BufferedWriter(fw);
    // if (deals.size() > 0) {
    // for (UFODeal deal : deals) {
    // String dealValue = "{\"type\":" + deal.value.get("type");
    // if (deal.value.containsKey("max_amount"))
    // dealValue += ",\"max_amount\":" + deal.value.get("max_amount");
    // if (deal.value.containsKey("percentage"))
    // dealValue += ",\"percentage\":" + deal.value.get("percentage");
    // dealValue += "}";
    //
    // // INSERT INTO table_name ( field1, field2,...fieldN ) VALUES ( value1, value2,...valueN );
    // String insertStr = "insert into t_deal (id, created_time, flag, merchant_id, claim_count, description, end_time,
    // image, max_count, max_count_per_user, mini_spend, start_time, title, value) values (\'"
    // + deal.id + "\',\'" + toTimeStr(deal.created_time) + "\',\'" + deal.flag + "\',\'"
    // + deal.merchant_id + "\',\'" + deal.claim_count + "\',\'"
    // + deal.description.replaceAll("'", "\\\\\'") + "\',\'" + deal.end_time + "\',\'"
    // + deal.image + "\',\'" + deal.max_count + "\',\'" + deal.max_count_per_user + "\',\'"
    // + deal.mini_spend + "\',\'" + deal.start_time + "\',\'"
    // + deal.title.replaceAll("'", "\\\\\'") + "\',\'" + dealValue + "\');\r";
    // bw.write(insertStr);
    // // System.out.println();
    // }
    // }
    // bw.close();
    // fw.close();
    // System.out.println("done: total = " + totalCount + ", valid = " + deals.size() + ", deleted = "
    // + deletedCount + ", expired = " + (totalCount - deletedCount - deals.size()));
    // } catch (Exception e) {
    // // TODO: handle exception
    // }
    // }

    private static ActiveType getDealStatus(HashMap<String, String> hDeal) {
        ActiveType result;
        String deletedDateStr = hDeal.get("DeletedDate");
        String endDateStr = hDeal.get("EndDate");
        long endDate = getLongTime(endDateStr);

        if (deletedDateStr == null || deletedDateStr.isEmpty()) {
            if (endDate == 0 || endDate > DataManager.getInstance().dbtime()) {
                result = ActiveType.EActive;
            } else {
                result = ActiveType.EExpired;
            }
        } else {
            result = ActiveType.EDeleted;
        }
        return result;
    }

    // 0: Event, 1: Amount, 2: Percentage
    private static DealType getDealType(HashMap<String, String> hDeal) {
        DealType result = DealType.EEvent;
        long percentage = 0, maxValue = 0;

        if (hDeal.get("PercentageValue") != null)
            percentage = Long.parseLong(hDeal.get("PercentageValue"));
        if (hDeal.get("MaxValue") != null)
            maxValue = Long.parseLong(hDeal.get("MaxValue"));

        if (percentage != 0) {
            result = DealType.EPersentage;
        } else {
            if (maxValue > 0)
                result = DealType.EAmount;
        }
        return result;
    }

    private static Integer intTypeFromDealType(DealType type) {
        Integer result = 0;
        if (type == DealType.EAmount)
            result = 1;
        if (type == DealType.EPersentage)
            result = 2;
        return result;
    }

    private static long getLongTime(String timeStr) {
        if (timeStr.isEmpty())
            return 0;
        if (timeStr.contains(".")) {
            String[] timeStrs = timeStr.split("[.]");
            timeStr = timeStrs[0];
        }
        TimeZone tz = TimeZone.getTimeZone("UTC");
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        sdf.setTimeZone(tz);
        try {
            java.util.Date date = (java.util.Date) sdf.parse(timeStr);
            return date.getTime();
        } catch (Exception e) {
            // TODO: handle exception
            System.out.println(e);
            return 0l;
        }

    }

    private static String toTimeStr(long time) {
        TimeZone tz = TimeZone.getTimeZone("UTC");
        DateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        sdf.setTimeZone(tz);
        try {
            return sdf.format(time);
        } catch (Exception e) {
            // TODO: handle exception
            return null;
        }
    }

    private static void initPVenuesSet() {
        ArrayList<HashMap<String, String>> aVenues = parseCSV(
                "/Users/justek/Public/ufo/Hiccup_Transfer/download/t_merchant.csv");
        for (int i = 0; i < aVenues.size(); i++) {
            HashMap<String, String> venue = aVenues.get(i);
            _pVenuesSet.add(venue.get("d"));
        }
    }

    private static void initPVenuesMap() {
        ArrayList<HashMap<String, String>> hVenues = parseCSV(
                "/Users/justek/Public/ufo/Hiccup_Transfer/download/ExternalVenues_1-30-2.csv");
        for (int i = 0; i < hVenues.size(); i++) {
            HashMap<String, String> venue = hVenues.get(i);
            String ApiKey = venue.get("ApiKeyId");
            if (ApiKey != null && ApiKey.equals("31910ec7-8d21-4de3-94be-c3487a03a121")
                    && venue.get("ExternalId") != null && venue.get("VenueId") != null)
                _pVenuesMap.put(venue.get("VenueId"), venue.get("ExternalId"));
        }
    }

    private static boolean isValidMerchant(String hVenueId) {
        if (hVenueId == null)
            return false;
        return (_pVenuesMap.containsKey(hVenueId) && _pVenuesSet.contains(_pVenuesMap.get(hVenueId)));
    }

    // private static void generateMerchant() {
    // ArrayList<FMerchant> merchants = new ArrayList<>();
    //
    // ArrayList<HashMap<String, String>> rMerchants = parseCSV("/Users/justek/Desktop/V1_Business.csv");
    // for (int i = 0; i < rMerchants.size(); i++) {
    // HashMap<String, String> rMerchant = rMerchants.get(i);
    // Long isVip = Long.parseLong(rMerchant.get("IsVip"));
    // if (isVip == 1) {
    // // totalCount++;
    // FMerchant fMerchant = new FMerchant();
    // fMerchant.id = rMerchant.get("usinessID");
    // fMerchant.name = rMerchant.get("BusinessName");
    // fMerchant.description = rMerchant.get("Description");
    // fMerchant.image_url = rMerchant.get("ImageUrl");
    // fMerchant.phone = rMerchant.get("Phone");
    // fMerchant.google_id = rMerchant.get("GoogleID");
    // fMerchant.yelp_id = rMerchant.get("YelpID");
    // fMerchant.google_rating = Float.parseFloat(rMerchant.get("GoogleRating"));
    // fMerchant.yelp_rating = Float.parseFloat(rMerchant.get("YelpRating"));
    // fMerchant.video_count = Long.parseLong(rMerchant.get("VideoCount"));
    // fMerchant.is_vip = 1;
    // fMerchant.latitude = Double.parseDouble(rMerchant.get("Latitude"));
    // fMerchant.longitude = Double.parseDouble(rMerchant.get("Longitude"));
    // fMerchant.address = rMerchant.get("Address");
    // fMerchant.deal_value = rMerchant.get("DealValue");
    // fMerchant.flag = 0;
    // fMerchant.google_reference = rMerchant.get("GoogleReference");
    // fMerchant.integration_token = rMerchant.get("IntegrationToken");
    // fMerchant.recommendation_recommend = 0l;
    // fMerchant.recommendation_total = 0l;
    // fMerchant.created_at = DataManager.getInstance().dbtime();
    // fMerchant.updated_at = fMerchant.created_at;
    // merchants.add(fMerchant);
    // }
    // }
    //
    // File file = new File("/Users/justek/Desktop/merchant_merge_sql.txt");
    // // 文件不存在时候，主动穿件文件。
    // if (!file.exists()) {
    // try {
    // file.createNewFile();
    // } catch (Exception e) {
    // // TODO: handle exception
    // }
    // }
    // try {
    // FileWriter fw = new FileWriter(file, false);
    // BufferedWriter bw = new BufferedWriter(fw);
    // if (merchants.size() > 0) {
    // for (FMerchant merchant : merchants) {
    //
    // // INSERT INTO table_name ( field1, field2,...fieldN ) VALUES ( value1, value2,...valueN );
    // // String insertStr = "insert into t_deal (id, created_time, flag, merchant_id, claim_count,
    // // description, end_time, image, max_count, max_count_per_user, mini_spend, start_time, title,
    // // value) values (\'"
    // // + deal.id + "\',\'" + toTimeStr(deal.created_time) + "\',\'" + deal.flag + "\',\'"
    // // + deal.merchant_id + "\',\'" + deal.claim_count + "\',\'"
    // // + deal.description.replaceAll("'", "\\\\\'") + "\',\'" + deal.end_time + "\',\'"
    // // + deal.image + "\',\'" + deal.max_count + "\',\'" + deal.max_count_per_user + "\',\'"
    // // + deal.mini_spend + "\',\'" + deal.start_time + "\',\'"
    // // + deal.title.replaceAll("'", "\\\\\'") + "\',\'" + dealValue + "\');\r";
    // // bw.write(insertStr);
    // //
    // SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:sss");
    // String insertStr = "INSERT INTO `t_merchant` (`id`, `name`, `description`, `image_url`, `phone`,"
    // + "`google_id`, `yelp_id`, `google_rating`, `yelp_rating`, `video_count`, `is_vip`, `latitude`,"
    // + "`longitude`, `address`, `deal_value`, `created_at`, `updated_at`, `flag`, `google_reference`,"
    // + "`integration_token`, `owner_id`, `rate_level`, `recommendation_recommend`, `recommendation_total`)"
    // + "VALUES" + "(\'" + merchant.id + "\', \'" + merchant.name + "\', NULL, \'"
    // + merchant.image_url + "\', \'" + merchant.phone + "\', \'" + merchant.google_id + "\', \'"
    // + merchant.yelp_id + "\', \'" + merchant.google_rating + "\', \'" + merchant.yelp_rating
    // + "\', \'" + merchant.video_count + "\', \'" + merchant.is_vip + "\', \'"
    // + merchant.latitude + "\', \'" + merchant.longitude + "\', \'" + merchant.address + "\', \'"
    // + merchant.deal_value + "\', \'" + Timestamp.valueOf(toTimeStr(merchant.created_at))
    // + "\', \'" + Timestamp.valueOf(sdf.format(new Date(merchant.updated_at))) + "\', \'"
    // + merchant.flag + "\', \'" + merchant.google_reference + "\', \'"
    // + merchant.integration_token + "\', \'" + merchant.owner_id + "\', \'0.0\', \'"
    // + merchant.recommendation_recommend + "\', \'" + merchant.recommendation_total + "\');";
    // System.out.println(insertStr);
    // }
    // }
    // bw.close();
    // fw.close();
    // // System.out.println("done: total = " + totalCount + ", valid = " + deals.size() + ", deleted = "
    // // + deletedCount + ", expired = " + (totalCount - deletedCount - deals.size()));
    // } catch (Exception e) {
    // // TODO: handle exception
    // }
    // }

    // private static void generateDeals() {
    // ArrayList<ADSAdvertisement> deals = new ArrayList<>();
    // AlphaId _alphaId = AlphaId.Default();
    // ArrayList<HashMap<String, String>> rDeals = parseCSV("/Users/justek/Desktop/V1_Deal.csv");
    // for (int i = 0; i < rDeals.size(); i++) {
    // HashMap<String, String> rDeal = rDeals.get(i);
    // // Long isVip = Long.parseLong(rMerchant.get("IsVip"));
    // ADSAdvertisement fDeal = new ADSAdvertisement();
    // fDeal.id = "D_" + _alphaId.getEncodeId(1000);
    // fDeal.title = rDeal.get("Title");
    // fDeal.description = rDeal.get("Description");
    // fDeal.image_url = rDeal.get("ImageUrl");
    // fDeal.value = Long.parseLong(rDeal.get("Value"));
    // fDeal.maxcount = Long.parseLong(rDeal.get("MaxCount"));
    // fDeal.maxcount_peruser = Long.parseLong(rDeal.get("MaxCountPerUser"));
    // fDeal.maxcount_remained = Long.parseLong(rDeal.get("MaxCountRemain"));
    // fDeal.merchant_id = rDeal.get("BusinessID");
    // fDeal.type = Integer.parseInt(rDeal.get("Type"));
    // fDeal.start_time = Long.parseLong(rDeal.get("StartTime"));
    // fDeal.end_time = Long.parseLong(rDeal.get("EndTime"));
    // fDeal.created_at = DataManager.getInstance().dbtime();
    // fDeal.updated_at = fDeal.created_at;
    // fDeal.flag = 0;
    // deals.add(fDeal);
    // }
    //
    // File file = new File("/Users/justek/Desktop/merchant_merge_sql.txt");
    // // 文件不存在时候，主动穿件文件。
    // if (!file.exists()) {
    // try {
    // file.createNewFile();
    // } catch (Exception e) {
    // // TODO: handle exception
    // }
    // }
    // try {
    // FileWriter fw = new FileWriter(file, false);
    // BufferedWriter bw = new BufferedWriter(fw);
    // if (deals.size() > 0) {
    // for (ADSAdvertisement deal : deals) {
    //
    // // INSERT INTO table_name ( field1, field2,...fieldN ) VALUES ( value1, value2,...valueN );
    // // String insertStr = "insert into t_deal (id, created_time, flag, merchant_id, claim_count,
    // // description, end_time, image, max_count, max_count_per_user, mini_spend, start_time, title,
    // // value) values (\'"
    // // + deal.id + "\',\'" + toTimeStr(deal.created_time) + "\',\'" + deal.flag + "\',\'"
    // // + deal.merchant_id + "\',\'" + deal.claim_count + "\',\'"
    // // + deal.description.replaceAll("'", "\\\\\'") + "\',\'" + deal.end_time + "\',\'"
    // // + deal.image + "\',\'" + deal.max_count + "\',\'" + deal.max_count_per_user + "\',\'"
    // // + deal.mini_spend + "\',\'" + deal.start_time + "\',\'"
    // // + deal.title.replaceAll("'", "\\\\\'") + "\',\'" + dealValue + "\');\r";
    // // bw.write(insertStr);
    // //
    // SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:sss");
    // // String insertStr = "INSERT INTO `t_merchant` (`id`, `name`, `description`, `image_url`, `phone`,"
    // // + "`google_id`, `yelp_id`, `google_rating`, `yelp_rating`, `video_count`, `is_vip`, `latitude`,"
    // // + "`longitude`, `address`, `deal_value`, `created_at`, `updated_at`, `flag`, `google_reference`,"
    // // + "`integration_token`, `owner_id`, `rate_level`, `recommend_count`, `recommend_level`)"
    // // + "VALUES" + "(\'" + merchant.id + "\', \'" + merchant.name + "\', NULL, \'"
    // // + merchant.image_url + "\', \'" + merchant.phone + "\', \'" + merchant.google_id + "\', \'"
    // // + merchant.yelp_id + "\', \'" + merchant.google_rating + "\', \'" + merchant.yelp_rating
    // // + "\', \'" + merchant.video_count + "\', \'" + merchant.is_vip + "\', \'"
    // // + merchant.latitude + "\', \'" + merchant.longitude + "\', \'" + merchant.address + "\', \'"
    // // + merchant.deal_value + "\', \'" + Timestamp.valueOf(toTimeStr(merchant.created_at))
    // // + "\', \'" + Timestamp.valueOf(sdf.format(new Date(merchant.updated_at))) + "\', \'"
    // // + merchant.flag + "\', \'" + merchant.google_reference + "\', \'"
    // // + merchant.integration_token + "\', \'" + merchant.owner_id + "\', \'0.0\', \'"
    // // + merchant.recommend_count + "\', \'" + merchant.recommend_level + "\');";
    //
    // String insertStr = "INSERT INTO `t_deal` (`id`, `title`, `description`, `image_url`, `value`, "
    // + "`maxcount`, `maxcount_peruser`, `maxcount_remained`, `merchant_id`, `type`, `start_time`, "
    // + "`end_time`, `created_at`, `updated_at`, `flag`) VALUES" + "(\'" + deal.id + "\', \'"
    // + deal.title + "\', \'" + deal.description + "\', \'" + deal.image_url + "\', \'"
    // + deal.value + "\', \'" + deal.maxcount + "\', \'" + deal.maxcount_peruser + "\', \'"
    // + deal.maxcount_remained + "\', \'" + deal.merchant_id + "\', \'" + deal.type + "\', \'"
    // + Timestamp.valueOf(toTimeStr(deal.start_time * 1000)) + "\', \'"
    // + Timestamp.valueOf(toTimeStr(deal.end_time * 1000)) + "\', \'"
    // + Timestamp.valueOf(toTimeStr(deal.created_at)) + "\', \'"
    // + Timestamp.valueOf(sdf.format(new Date(deal.updated_at))) + "\', \'" + deal.flag + "\');";
    // System.out.println(insertStr);
    // }
    // }
    // bw.close();
    // fw.close();
    // // System.out.println("done: total = " + totalCount + ", valid = " + deals.size() + ", deleted = "
    // // + deletedCount + ", expired = " + (totalCount - deletedCount - deals.size()));
    // } catch (Exception e) {
    // // TODO: handle exception
    // }
    // }

    // public static String exportDataToCSV(ArrayList<ADSData> data, String... headers) throws IOException {
    // StringWriter sw = new StringWriter();
    // CSVPrinter csvPrinter = new CSVPrinter(sw, CSVFormat.DEFAULT.withHeader(headers));
    //
    // for (StandaloneArgument argument : arguments) {
    // csvPrinter.printRecord(argument.getId(), argument.getAuthor(), argument.getAnnotatedStance(),
    // argument.getTimestamp(), argument.getDebateMetaData().getTitle(),
    // argument.getDebateMetaData().getDescription(), argument.getDebateMetaData().getUrl());
    // }
    //
    // sw.flush();
    //
    // return sw.toString();
    // }

    public static void exportDataToCSV(String fileName, JsonArray data) throws IOException {
        if (data == null || data.size() == 0) {
            log.warn("Invalid data to export.");
            return;
        }

        FileWriter fw = new FileWriter(fileName);
        ArrayList<String> headers = new ArrayList<>();

        data.getJsonObject(0).forEach(entry -> {
            headers.add(entry.getKey());
        });

        CSVPrinter csvPrinter = new CSVPrinter(fw,
                CSVFormat.DEFAULT.withHeader((String[]) headers.toArray(new String[0])));
        for (int i = 0; i < data.size(); i++) {
            JsonObject d = data.getJsonObject(i);
            ArrayList<Object> value = new ArrayList<>();
            d.forEach(entry -> {
                value.add(entry.getValue());
            });
            csvPrinter.printRecord(value.toArray());
        }
        fw.flush();
        fw.close();
        csvPrinter.close();
    }
}

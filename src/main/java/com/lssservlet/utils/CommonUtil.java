package com.lssservlet.utils;

import java.net.URLDecoder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import org.apache.http.util.TextUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.lssservlet.core.DataManager;
import com.lssservlet.datamodel.ADSData;

public class CommonUtil {
    protected static final Logger log = LogManager.getLogger(ADSData.class);

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

    public static final Long TAX_UNIT = 10_000_000L;
    public static final Long PRICE_UNIT = 100L;
    public static final Long WEIGHT_UNIT = 100L;
    public static final Long QUANTITY_UNIT = 100L;
    public static final Long TIME_UNIT = 1000L;

    public static int weekOfYear() {
        int weekNum = 0;
        SimpleDateFormat format = new SimpleDateFormat("w", Locale.US);
        String week = format.format(new Date(DataManager.getInstance().dbtime()));
        weekNum = Integer.parseInt(week);
        return weekNum;
    }

    public static String decode(String content, int decodeTime) {
        try {
            while (decodeTime > 0) {
                content = URLDecoder.decode(content, "utf-8");
                decodeTime--;
            }
        } catch (Exception e) {
            log.error("Fail to decode: {}, msg: ", content, e.getMessage());
        }
        return content;
    }

    public static Long decodeToDate(String content, int decodeTime) {
        Long time = 0L;
        try {
            while (decodeTime > 0) {
                content = URLDecoder.decode(content, "utf-8");
                decodeTime--;
            }

            time = DATE_FORMAT.parse(content).getTime();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return time;
    }

    public static Long decodeToLong(String content, Long unit, Long def) {
        if (!TextUtils.isEmpty(content)) {
            try {
                content = URLDecoder.decode(content, "utf-8");
                Double ret = Double.parseDouble(content);
                return (long) (ret * unit);
            } catch (Exception e) {
                // Throw this exception when content = "n/a"
                e.printStackTrace();
            }
        }

        return def;
    }

    public static Long changeToLong(String value, Long unit, Long def) {
        try {
            if (value != null && value.length() > 0) {
                return (long) (Double.parseDouble(value) * unit);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return def;
    }

    public static Long changeToLong(Double value, Long unit, Long def) {
        try {
            if (value != null) {
                return (long) (value * unit);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return def;
    }

    public static Long convertToPrice(Double value, Long def) {
        if (value != null) {
            return (long) (value * PRICE_UNIT);
        }

        return def;
    }

    public static Long convertToWeight(Double value, Long def) {
        if (value != null) {
            return (long) (value * WEIGHT_UNIT);
        }

        return def;
    }

    public static Long convertToQuantity(Double value, Long def) {
        if (value != null) {
            return (long) (value * QUANTITY_UNIT);
        }

        return def;
    }

    public static Long convertToTax(Double value, Long def) {
        if (value != null) {
            return (long) (value * TAX_UNIT);
        }

        return def;
    }

    public static Long convertToTime(Long time) {
        return time * TIME_UNIT;
    }
}

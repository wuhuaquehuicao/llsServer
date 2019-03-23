package com.lssservlet.utils;

import java.math.BigInteger;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicLong;

public class AlphaId {
    private static final AtomicLong LAST_TimeMillis = new AtomicLong();
    private static final AtomicLong LAST_COUNT = new AtomicLong();

    enum DICTIONARY {
        E32("123456789ABCDEFGHJKMNPQRSTUVWXYZ"),
        E36("0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ"),
        E62("0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz");

        private String _value = "";

        DICTIONARY(String v) {
            _value = v;
        }

        String getValue() {
            return _value;
        }
    }

    protected char[] dictionary;

    public static AlphaId Dictionary32() {
        return new AlphaId(DICTIONARY.E32);
    }

    public static AlphaId Dictionary36() {
        return new AlphaId(DICTIONARY.E36);
    }

    public static AlphaId Dictionary62() {
        return new AlphaId(DICTIONARY.E62);
    }

    public static AlphaId Default() {
        return AlphaId.Dictionary62();
    }

    private AlphaId(DICTIONARY dict) {
        this.dictionary = dict.getValue().toCharArray();
    }

    public static long getUniqueId() {
        while (true) {
            long now = System.currentTimeMillis();
            long lastTime = LAST_TimeMillis.get();
            long lastCount = 0;
            if (lastTime != now) {
                LAST_COUNT.set(0);
            } else {
                lastCount = LAST_COUNT.incrementAndGet();
            }
            if (lastCount > 999) {
                continue;
            }
            if (LAST_TimeMillis.compareAndSet(lastTime, now) && LAST_COUNT.get() == lastCount)
                return now * 1000 + lastCount;
        }
    }

    public static BigInteger getUniqueId(long serverId) {
        long now = getUniqueId();
        return new BigInteger(String.valueOf(serverId) + String.valueOf(now));
    }

    public String getEncodeId(long serverId) {
        return encode(getUniqueId(serverId));
    }

    public BigInteger getMinValue(int bit) {
        String minString = null;
        if (dictionary[0] == '0')
            minString = String.valueOf(dictionary[1]);
        else
            minString = String.valueOf(dictionary[0]);
        int count = 1;
        while (count < bit) {
            minString += String.valueOf(dictionary[0]);
            count++;
        }
        return decode(minString);
    }

    public BigInteger getMaxValue(int bit) {
        String minString = String.valueOf(dictionary[dictionary.length - 1]);
        int count = 1;
        while (count < bit) {
            minString += String.valueOf(dictionary[dictionary.length - 1]);
            count++;
        }
        return decode(minString);
    }

    public static void main(String[] args) {
        AlphaId bx = AlphaId.Default();
        // BigInteger v1 = bx.getMinValue(13); // 13 --32 2626 6762397899821056
        // BigInteger v2 = bx.getMaxValue(13); // 13 2000 2853 9268669788905471
        // seed server id uniqueid ------------------ 999 1000 1479093251036066
        // 136 1050 1097850022745562
        long startTime = System.nanoTime();
        long count = 0;
        long maxPerMilSec = 0;
        // 5s
        while (System.nanoTime() - startTime < 5 * 1000000000l) {
            long r = System.nanoTime() % 1000;
            BigInteger v = getUniqueId((r < 100 ? (r + 100) : r) * 10000 + 1000);
            String original = v.toString();
            System.out.println("Original: " + original + " , len=" + original.length());
            String perMilSec = original.substring(original.length() - 3, original.length());
            if (Long.parseLong(perMilSec) > maxPerMilSec)
                maxPerMilSec = Long.parseLong(perMilSec);
            String encoded = bx.encode(new BigInteger(original));
            System.out.println("encoded: " + encoded);
            BigInteger decoded = bx.decode(encoded);
            System.out.println("decoded: " + decoded);
            if (original.equals(decoded.toString()) && encoded.length() == 13) {
                System.out.println("Passed! decoded value is the same as the original.");
                count++;
            } else {
                System.err.println("Failed! decoded value is NOT the same as the original!!");
            }
        }
        System.out.println(
                "Time:" + (System.nanoTime() - startTime) + " , count:" + count + " , maxPerMilSec:" + maxPerMilSec);
    }

    public String encode(String seed) {
        BigInteger num = new BigInteger(Codec.md5(seed), 16);
        return encode(num);
    }

    public String encode(BigInteger value) {
        List<Character> result = new ArrayList<Character>();
        BigInteger base = new BigInteger("" + dictionary.length);
        int exponent = 1;
        BigInteger remaining = value;
        while (true) {
            BigInteger a = base.pow(exponent); // 16^1 = 16
            BigInteger b = remaining.mod(a); // 119 % 16 = 7 | 112 % 256 = 112
            BigInteger c = base.pow(exponent - 1);
            BigInteger d = b.divide(c);

            result.add(dictionary[d.intValue()]);
            remaining = remaining.subtract(b); // 119 - 7 = 112 | 112 - 112 = 0
            // finished?
            if (remaining.equals(BigInteger.ZERO)) {
                break;
            }
            exponent++;
        }
        // need to reverse it, since the start of the list contains the least
        // significant values
        StringBuffer sb = new StringBuffer();
        for (int i = result.size() - 1; i >= 0; i--) {
            sb.append(result.get(i));
        }
        return sb.toString();
    }

    public BigInteger decode(String str) {
        // reverse it, coz its already reversed!
        char[] chars = new char[str.length()];
        str.getChars(0, str.length(), chars, 0);

        char[] chars2 = new char[str.length()];
        int i = chars2.length - 1;
        for (char c : chars) {
            chars2[i--] = c;
        }
        // for efficiency, make a map
        Map<Character, BigInteger> dictMap = new HashMap<Character, BigInteger>();
        int j = 0;
        for (char c : dictionary) {
            dictMap.put(c, new BigInteger("" + j++));
        }

        BigInteger bi = BigInteger.ZERO;
        BigInteger base = new BigInteger("" + dictionary.length);
        int exponent = 0;
        for (char c : chars2) {
            BigInteger a = dictMap.get(c);
            if (a != null) {
                BigInteger b = base.pow(exponent).multiply(a);
                bi = bi.add(new BigInteger("" + b));
                exponent++;
            } else {
                System.out.println(c);
            }
        }
        return bi;
    }

    public static String generateID() {
        String result = null;
        TimeZone tz = TimeZone.getTimeZone("GMT");
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
        sdf.setTimeZone(tz);
        result = sdf.format(new Date());

        Long ran = (long) (Math.random() * 90000 + 10000);
        result += String.valueOf(ran);
        return result;
    }
}

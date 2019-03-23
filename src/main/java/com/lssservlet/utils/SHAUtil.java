package com.lssservlet.utils;

import java.security.MessageDigest;

public class SHAUtil {

    private static final char[] HEX_DIGITS = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd',
            'e', 'f' };

    private static String getFormattedText(byte[] bytes) {
        int len = bytes.length;
        StringBuilder buf = new StringBuilder(len * 2);
        for (int index = 0; index < len; index++) {
            buf.append(HEX_DIGITS[(bytes[index] >> 4) & 0x0F]);
            buf.append(HEX_DIGITS[bytes[index] & 0x0F]);
        }
        return buf.toString();
    }

    public static String encode(String content) {
        String result = "";
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            digest.update(content.getBytes("UTF-8"));
            byte msgDigest[] = digest.digest();
            result = getFormattedText(msgDigest);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return result;
    }

    public static void main(String[] args) {
        SHAUtil util = new SHAUtil();
        String content = "Hello World";
        String result = util.encode(content);
        System.out.println("encode result: " + result);
    }
}

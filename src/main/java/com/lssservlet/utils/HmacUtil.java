package com.lssservlet.utils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public class HmacUtil {

    public static byte[] sha1(byte[] data, byte[] key) {
        try {
            SecretKeySpec signinKey = new SecretKeySpec(key, "HmacSHA1");
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(signinKey);
            return mac.doFinal(data);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public static void main(String[] args) {
        HmacUtil util = new HmacUtil();
        String key = "12345678";
        String data = "123456";
        byte[] result = util.sha1(data.getBytes(), key.getBytes());
        String content = Codec.Base64encode(result);
        System.out.println("Result: " + content);
    }
}

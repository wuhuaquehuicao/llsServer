package com.lssservlet.utils;

import java.io.UnsupportedEncodingException;
import java.util.UUID;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;
import org.mindrot.jbcrypt.BCrypt;

public class Codec {
    public static String Base64encode(byte[] source) {
        return new String(Base64.encodeBase64(source));
    }

    public static byte[] Base64decode(String source) {
        return Base64.decodeBase64(source);
    }

    public static String md5(byte[] s) {
        return new String(DigestUtils.md5Hex(s));
    }

    public static String md5(String s) {
        return new String(DigestUtils.md5Hex(s));
    }

    public static String sha256(String s) {
        return DigestUtils.sha256Hex(s);
    }

    public static String hmacSha256(String secret, String message) {
        try {
            Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
            SecretKeySpec secret_key = new SecretKeySpec(secret.getBytes(), "HmacSHA256");
            sha256_HMAC.init(secret_key);
            return convertToHex(sha256_HMAC.doFinal(message.getBytes("UTF-8")));
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        return null;
    }

    public static String urlEncode(JsonObject data) {
        StringBuilder sb = new StringBuilder();
        data.forEach(e -> {
            Object value = e.getValue();
            if (value instanceof String || value instanceof Number || value instanceof Boolean
                    || value instanceof JsonObject || value instanceof JsonArray) {
                String encode = null;
                try {
                    encode = java.net.URLEncoder.encode(value.toString(), "UTF-8");
                    // encode = value.toString();
                } catch (UnsupportedEncodingException e1) {
                    e1.printStackTrace();
                }
                if (encode != null) {
                    encode = encode.replaceAll("\\+", "%20");
                    sb.append(e.getKey());
                    sb.append("=");
                    sb.append(encode);
                    sb.append("&");
                }
            } else {
                System.out.println("unknown value");
            }
        });
        if (sb.length() > 0)
            sb.delete(sb.length() - 1, sb.length());
        return sb.toString();
    }

    public static String uuid() {
        return UUID.randomUUID().toString();
    }

    public static String uuid(String name) {
        return UUID.nameUUIDFromBytes(name.getBytes()).toString();
    }

    public static String bcryptHash(String secret) {
        return BCrypt.hashpw(secret, BCrypt.gensalt());
    }

    public static Boolean bcryptCheck(String secret, String hash) {
        if (secret != null && hash != null && secret.length() > 0 && hash.length() > 0 && BCrypt.checkpw(secret, hash))
            return true;
        return false;
    }

    private static String convertToHex(byte[] data) {
        StringBuilder buf = new StringBuilder();
        for (byte b : data) {
            int halfbyte = (b >>> 4) & 0x0F;
            int two_halfs = 0;
            do {
                buf.append(
                        (0 <= halfbyte) && (halfbyte <= 9) ? (char) ('0' + halfbyte) : (char) ('a' + (halfbyte - 10)));
                halfbyte = b & 0x0F;
            } while (two_halfs++ < 1);
        }

        return buf.toString();
    }
}
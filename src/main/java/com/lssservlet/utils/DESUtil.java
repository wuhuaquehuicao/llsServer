package com.lssservlet.utils;

import java.nio.charset.Charset;
import java.security.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;

public class DESUtil {

    private SecretKey _securekey = null;
    private SecureRandom _random = null;

    public static DESUtil create(String password) throws Exception {
        SecureRandom random = new SecureRandom();
        String md5 = Codec.md5(password);
        DESKeySpec desKey = new DESKeySpec(md5.getBytes("UTF-8"));
        SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("DES");
        SecretKey securekey = keyFactory.generateSecret(desKey);
        return new DESUtil(securekey, random);
    }

    private DESUtil(SecretKey securekey, SecureRandom random) {
        _securekey = securekey;
        _random = random;
    }

    public byte[] encrypt(byte[] data) {
        try {
            Cipher cipher = Cipher.getInstance("DES");
            cipher.init(Cipher.ENCRYPT_MODE, _securekey, _random);
            return cipher.doFinal(data);
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return null;
    }

    public byte[] decrypt(byte[] src) {
        try {
            Cipher cipher = Cipher.getInstance("DES");
            cipher.init(Cipher.DECRYPT_MODE, _securekey, _random);
            return cipher.doFinal(src);
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return null;
    }

    public String base64Encrypt(String data) {
        return Codec.Base64encode(encrypt(data.getBytes(Charset.forName("utf-8"))));
    }

    public String base64Decrypt(String src) {
        return new String(decrypt(Codec.Base64decode(src)), Charset.forName("utf-8"));
    }

    public static void main(String[] args) throws Exception {
    }
}

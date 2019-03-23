package com.lssservlet.utils;

import java.nio.charset.Charset;
import java.security.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

public class AESUtil {
    private SecretKey _securekey = null;

    private static final String password = "wcdwbluudgr88ukb";
    // private static final byte[] init_vector_bytes = "wcdwbluudgr88ukb".getBytes();
    // private static final String pass_phrase = "sepzq7f3";

    private static volatile AESUtil sInstance = null;

    private AESUtil(String password) throws Exception {
        KeyGenerator kgen = KeyGenerator.getInstance("AES");
        SecureRandom random = SecureRandom.getInstance("SHA1PRNG"); // new SecureRandom(password.getBytes());
        random.setSeed(password.getBytes());
        kgen.init(128, random);
        _securekey = kgen.generateKey();
    }

    public static AESUtil getInstance() {
        if (sInstance == null) {
            synchronized (AESUtil.class) {
                if (sInstance == null) {
                    try {
                        sInstance = new AESUtil(password);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        return sInstance;
    }

    public AESUtil create(String password) throws Exception {
        KeyGenerator kgen = KeyGenerator.getInstance("AES");
        SecureRandom random = SecureRandom.getInstance("SHA1PRNG"); // new SecureRandom(password.getBytes());
        random.setSeed(password.getBytes());
        kgen.init(128, random);
        SecretKey secretKey = kgen.generateKey();
        return new AESUtil(secretKey);
    }

    private AESUtil(SecretKey securekey) {
        _securekey = securekey;
    }

    public byte[] encrypt(byte[] data) {
        try {
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, _securekey);
            return cipher.doFinal(data);
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return null;
    }

    public byte[] decrypt(byte[] src) {
        try {
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.DECRYPT_MODE, _securekey);
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
}

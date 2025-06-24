package com.bgpay.bgai.utils;

import java.security.MessageDigest;
import java.util.Base64;

public class Sha256Util {
    public static String hash(String data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data.getBytes());
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException("SHA-256哈希失败", e);
        }
    }
} 
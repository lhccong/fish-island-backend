package com.cong.fishisland.utils;

import lombok.extern.slf4j.Slf4j;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;

/**
 * 加密工具类
 * @author 许林涛
 * @date 2025年05月16日 9:12
 */
@Slf4j
public class CryptoUtils {
    private static final String AES_KEY = "FishIslandKey_16";
    private static final String AES_IV = "FishIsland_IV_16";


    /**
     * 安全加密方法（自动捕获异常）
     * @param data 明文数据
     * @return 成功返回密文，失败返回null
     */
    public static String encryptSafely(String data) {
        try {
            return encrypt(data);
        } catch (Exception e) {
            log.error("加密操作失败 | 原因：{} | 数据：{}",
                    e.getClass().getSimpleName(),
                    data.substring(0, Math.min(data.length(), 50)),  // 防止日志泄露敏感信息
                    e
            );
            return null;
        }
    }

    /**
     * 安全解密方法（自动捕获异常）
     * @param encryptedData 密文数据
     * @return 成功返回明文，失败返回null
     */
    public static String decryptSafely(String encryptedData) {
        try {
            return decrypt(encryptedData);
        } catch (Exception e) {
            log.error("解密操作失败 | 原因：{} | 密文：{}",
                    e.getClass().getSimpleName(),
                    encryptedData.length() > 100 ?
                            encryptedData.substring(0, 50) + "..." : encryptedData,
                    e
            );
            return null;
        }
    }
    /**
     * 加密
     * @param data 需要加密的数据
     * @return 加密后的数据
     */
    private static String encrypt(String data) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        SecretKeySpec keySpec = new SecretKeySpec(AES_KEY.getBytes(), "AES");
        IvParameterSpec ivSpec = new IvParameterSpec(AES_IV.getBytes());
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);
        byte[] encrypted = cipher.doFinal(data.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(encrypted);
    }

    /**
     * 解密
     * @param encryptedData 加密后的Base64字符串
     * @return 解密后的原始数据
     */
    private static String decrypt(String encryptedData) throws Exception {
        // 统一使用UTF-8编码获取字节
        byte[] keyBytes = AES_KEY.getBytes(StandardCharsets.UTF_8);
        byte[] ivBytes = AES_IV.getBytes(StandardCharsets.UTF_8);

        // 强制长度校验（防止配置错误）
        validateKeyLength(keyBytes, "AES_KEY", 16, 24, 32);
        validateKeyLength(ivBytes, "AES_IV", 16);

        // 创建解密器
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "AES");
        IvParameterSpec ivSpec = new IvParameterSpec(ivBytes);
        cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);

        // 解码Base64并进行解密
        byte[] encryptedBytes = Base64.getDecoder().decode(encryptedData);
        byte[] decryptedBytes = cipher.doFinal(encryptedBytes);

        return new String(decryptedBytes, StandardCharsets.UTF_8);
    }

    /**
     * 密钥长度验证
     */
    private static void validateKeyLength(byte[] bytes, String name, int... validLengths) {
        for (int len : validLengths) {
            if (bytes.length == len) return;
        }
        throw new IllegalArgumentException(
                String.format("%s长度错误：当前%d字节，要求：%s",
                        name,
                        bytes.length,
                        Arrays.toString(validLengths))
        );
    }
}

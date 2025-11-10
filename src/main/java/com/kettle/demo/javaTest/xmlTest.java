package com.kettle.demo.javaTest;


import javax.crypto.Cipher;
import java.security.*;
import java.security.spec.*;
import java.io.*;
import java.time.Instant;
import java.util.Base64;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * 用Java实现数据调用测试（返回页面的形式）
 */

public class xmlTest {

    public static void main(String[] args) throws Exception {

        String publicKeyPath = "C:\\Users\\Dell\\Desktop\\key.pub";
        PublicKey publicKey = RSAPublicKeyReader.readPublicKey(publicKeyPath);
        Instant instant = Instant.now();
        // 使用java.time.Instant获取当前时间戳（毫秒）
        long timestampMillis = instant.toEpochMilli();




        System.out.println("时间戳："+timestampMillis);

        String s = "clientId=q_client24&idCard=610528199211090033&idType=01&nonce=qA75EBuk1dacjHRG&secret=cbf41a26e8bb4d75a083a0c8d2cca5b2&timestamp=" + timestampMillis;
        System.out.println(s);
        String s1 = sha1(s);

        // The string you want to encrypt
        String data = "{\"info\":{\"clientId\":\"q_client24\",\"secret\":\"cbf41a26e8bb4d75a083a0c8d2cca5b2\",\"idType\":\"01\",\"idCard\":\"610528199211090033\"},\"timestamp\":\"#\",\"nonce\":\"qA75EBuk1dacjHRG\",\"signature\":\"@\"}"
                .replace("#", String.valueOf(timestampMillis))
                .replace("@", s1);
        System.out.println(data);


        // Encrypt the data
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);
        byte[] encryptedData = cipher.doFinal(data.getBytes());

        // Encode the encrypted data to Base64
        String base64EncodedEncryptedData = Base64.getEncoder().encodeToString(encryptedData);
        System.out.println("生成code:  " + base64EncodedEncryptedData);
    }

    private static String sha1(String s) {
        try {
            // Create a SHA1 digest instance
            MessageDigest digest = MessageDigest.getInstance("SHA-1");

            // Perform the digest on the string's bytes
            byte[] hashBytes = digest.digest(s.getBytes());

            // Convert the digest to a hex string format
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            // Print out the SHA1 hash
            System.out.println("SHA1加密后: " + hexString.toString());
            return hexString.toString();

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return null;
    }
}


class RSAPublicKeyReader {

    /**
     * Reads a PEM encoded RSA public key.
     */
    public static PublicKey readPublicKey(String publicKeyPEMPath) throws Exception {
        String publicKeyPEM = new String(Files.readAllBytes(Paths.get(publicKeyPEMPath)));

        // strip of header, footer, newlines, whitespaces
        publicKeyPEM = publicKeyPEM.replace("-----BEGIN RSA PUBLIC KEY-----", "");
        publicKeyPEM = publicKeyPEM.replace("-----END RSA PUBLIC KEY-----", "");
        publicKeyPEM = publicKeyPEM.replaceAll("\\s", "");

        // decode to get the binary DER representation
        byte[] publicKeyDER = Base64.getDecoder().decode(publicKeyPEM);

        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        PublicKey publicKey = keyFactory.generatePublic(new X509EncodedKeySpec(publicKeyDER));

        return publicKey;
    }
}















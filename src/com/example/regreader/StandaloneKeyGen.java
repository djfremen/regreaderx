package com.example.regreader;

import java.io.UnsupportedEncodingException;
import java.text.DecimalFormat;
import java.util.Locale;
import java.util.Random;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;

public class StandaloneKeyGen {
    private static InstallationType installationType;
    private static final Logger log = Logger.getLogger(StandaloneKeyGen.class);
    public static boolean CHECKMODE;
    private static String hardwareHashID;
    private static String hardwareID;
    private static int users;
    private static String authorizationID;
    private static AuthorizationStatus authorizationStatus;
    private static String subscriberID;
    private static long timestamp;
    private static byte[] timestamp1;
    private static Random random = new Random();

    public static void main(String[] args) throws Exception {
        ConsoleAppender appender = new ConsoleAppender(new PatternLayout());
        log.addAppender(appender);
        if (args.length > 0) {
            String[] subscriberIds = {"TIS2WEB", "GlobalTIS"};
            boolean success = false;
            
            for (String subId : subscriberIds) {
                try {
                    setSubscriberID(subId);
                    byte[] key = keyAES(subId);
                    decode(decryptAES(key, toBytes(args[0])));
                    success = true;
                    // If successful, stick with this subscriber ID for the rest of the process
                    // unless overridden by args[1]
                    break;
                } catch (javax.crypto.BadPaddingException e) {
                    // Failed with this ID, try next
                    continue;
                } catch (Exception e) {
                    // Other errors, log and continue
                    log.warn("Decryption failed with subscriber ID: " + subId, e);
                }
            }
            
            if (!success) {
                System.out.println("Error: Could not decrypt software key with known subscriber IDs.");
                return;
            }

            authorize();
            if (args.length > 1) {
                setSubscriberID(args[1]);
            } else {
                int rnd = Math.abs(random.nextInt(999999999));
                DecimalFormat df2 = new DecimalFormat("000000000");
                setSubscriberID("T" + df2.format((long)rnd));
            }

            // Print the subscriber ID
            System.out.println(getSubscriberID());

            // Print the license key
            System.out.println(encrypt(encode()));

            // Print additional information about this key
            System.out.println("     Standalone KeyGen      ");

            // Additional information from the z90.pl API format
            System.out.println("hwid: " + getHardwareHashID());
            System.out.println("installationType: " + getInstallationType());
            System.out.println("users: " + getUsers());
            System.out.println("activation: " + getAuthorizationID());
        } else {
            System.out.println("Enter request key");
            System.out.println("     Standalone KeyGen      ");
        }
        // System.exit(0) removed to prevent application termination
    }

    private static byte[] keyAES(String key) {
        try {
            return ("##" + pad(key, 10) + "####").getBytes("US-ASCII");
        } catch (UnsupportedEncodingException e) {
            log.error("getBytes() failed", e);
            return ("##" + pad(key, 10) + "####").getBytes();
        }
    }

    private static String pad(String value, int length) {
        if (value == null) value = "";
        StringBuffer buffer = new StringBuffer(value);
        while (buffer.length() < length) buffer.append('~');
        return buffer.toString().substring(0, length);
    }

    public static byte[] toBytes(String hex) {
        byte[] bytes = new byte[hex.length() / 2];
        for (int i = 0; i < hex.length() / 2; ++i) {
            bytes[i] = (byte) Integer.parseInt(hex.substring(i * 2, i * 2 + 2), 16);
        }
        return bytes;
    }

    private static byte[] decryptAES(byte[] key, byte[] encryption) throws Exception {
        SecretKeySpec skeySpec = new SecretKeySpec(key, "AES");
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(2, skeySpec);
        return cipher.doFinal(encryption);
    }

    public static String bytesToHex(boolean format, byte[] data, int length) {
        StringBuffer buffer = new StringBuffer();
        for (int i = 0; i < length; ++i) {
            buffer.append(toHex(data[i]));
            if (format && (i + 1) % 2 == 0 && i < length - 1) buffer.append("-");
        }
        return buffer.toString().toUpperCase(Locale.ENGLISH);
    }

    public static String toHex(byte b) {
        Integer I = new Integer(b << 24 >>> 24);
        int i = I;
        return i < 16 ? "0" + Integer.toString(i, 16) : Integer.toString(i, 16);
    }

    public static String bytesToHex(byte[] data) {
        return bytesToHex(true, data, data.length);
    }

    private static void decode(byte[] payload) {
        if (payload[23] == 0 && payload[24] == checksum(payload, 0, 22) &&
                payload[25] == (byte)(checksum(payload, 0, 30) - payload[25])) {
            byte[] hwid = new byte[10];
            copy(hwid, 0, payload);
            hardwareHashID = new String(hwid);
            installationType = InstallationType.get(payload[10]);
            users = payload[11];
            authorizationStatus = AuthorizationStatus.get(payload[12]);
            byte[] subscription = new byte[10];
            transfer(subscription, 13, payload);
            authorizationID = new String(subscription).replace('~', ' ').trim();
            byte[] ts = new byte[5];
            transfer(ts, 26, payload);
            timestamp1 = new byte[5];
            transfer(timestamp1, 26, payload);
            timestamp = Long.parseLong(new String(ts));
        }
    }

    private static byte checksum(byte[] data, int start, int stop) {
        int sum = 0;
        for (int i = start; i <= stop; ++i) sum += data[i];
        return (byte)(sum % 255);
    }

    private static void copy(byte[] target, int start, byte[] source) {
        int i = start;
        for (int j = 0; j < source.length && i < target.length; ++j) {
            target[i++] = source[j];
        }
    }

    private static void transfer(byte[] target, int start, byte[] source) {
        int i = start;
        for (int j = 0; i < source.length && j < target.length; ++j) {
            target[j] = source[i++];
        }
    }

    private static byte[] encode() {
        byte[] payload = new byte[31];
        try {
            copy(payload, 0, getHardwareHashID().getBytes("US-ASCII"));
        } catch (UnsupportedEncodingException e) {
            log.error("getBytes() failed", e);
            copy(payload, 0, getHardwareHashID().getBytes());
        }
        payload[10] = (byte) getInstallationType().ord();
        payload[11] = (byte) getUsers();
        payload[12] = (byte) getAuthorizationStatus().ord();
        try {
            copy(payload, 13, pad(getAuthorizationID(), 10).getBytes("US-ASCII"));
        } catch (UnsupportedEncodingException e) {
            log.error("getBytes() failed", e);
            copy(payload, 13, pad(getAuthorizationID(), 10).getBytes());
        }
        copy(payload, 26, timestamp1);
        payload[24] = checksum(payload, 0, 22);
        payload[25] = checksum(payload, 0, 30);
        return payload;
    }

    private static String encrypt(byte[] payload) throws Exception {
        byte[] key = keyAES(getSubscriberID());
        return bytesToHex(encryptAES(key, payload));
    }

    private static byte[] encryptAES(byte[] key, byte[] payload) throws Exception {
        SecretKeySpec skeySpec = new SecretKeySpec(key, "AES");
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(1, skeySpec);
        return cipher.doFinal(payload);
    }

    public static void setSubscriberID(String subscriberID1) {
        subscriberID = subscriberID1.toUpperCase(Locale.ENGLISH);
    }

    public static String getSubscriberID() { return subscriberID; }
    public static String getAuthorizationID() { return authorizationID; }
    public static void setAuthorizationID(String authorizationID1) { authorizationID = authorizationID1; }
    public static AuthorizationStatus getAuthorizationStatus() { return authorizationStatus; }
    public static String getHardwareHashID() { return hardwareHashID; }
    public static String getHardwareID() { return hardwareID; }
    public static int getUsers() { return users; }
    public static InstallationType getInstallationType() { return installationType; }
    public static void authorize() { authorizationStatus = AuthorizationStatus.AUTHORIZED; }
    public static long getTimestamp() { return timestamp; }
}
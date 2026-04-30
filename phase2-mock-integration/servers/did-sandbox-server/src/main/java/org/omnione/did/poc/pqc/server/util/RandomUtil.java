/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  lombok.Generated
 */
package org.omnione.did.poc.pqc.server.util;

import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.UUID;
import lombok.Generated;
import org.omnione.did.common.util.HexUtil;
import org.omnione.did.common.util.NonceGenerator;

public class RandomUtil {
    private static final SecureRandom secureRandom = new SecureRandom();
    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

    public static String generateUUID() {
        return UUID.randomUUID().toString();
    }

    public static String generateMessageId() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
        String currentDateAndTime = dateFormat.format(new Date());
        long currentTimeMillis = System.currentTimeMillis();
        long microsecond = currentTimeMillis % 1000L * 1000L;
        String randomHexString = RandomUtil.generateUUID().replace("-", "").substring(0, 8);
        return currentDateAndTime + String.format("%06d", microsecond) + randomHexString;
    }

    public static String generateWalletId() {
        String prefix = "WID";
        String datePart = RandomUtil.getCurrentYearMonth();
        String randomPart = RandomUtil.generateRandomString(11);
        return prefix + datePart + randomPart;
    }

    private static String getCurrentYearMonth() {
        return LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMM"));
    }

    public static String generateNonce() {
        return HexUtil.toHexString(NonceGenerator.generate16ByteNonce());
    }

    public static String generateRandomString(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; ++i) {
            int index = secureRandom.nextInt(CHARACTERS.length());
            sb.append(CHARACTERS.charAt(index));
        }
        return sb.toString();
    }

    @Generated
    private RandomUtil() {
    }
}


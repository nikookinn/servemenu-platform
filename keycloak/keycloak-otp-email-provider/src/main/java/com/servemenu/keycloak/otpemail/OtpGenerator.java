package com.servemenu.keycloak.otpemail;

import java.security.SecureRandom;
import java.time.LocalDateTime;

public class OtpGenerator {

    private static final SecureRandom random = new SecureRandom();
    private static final int OTP_LENGTH = 6;
    private static final int OTP_VALIDITY_MINUTES = 5;

    public static String generateOTP() {
        int otp = random.nextInt(900000) + 100000;
        return String.valueOf(otp);
    }

    public static long getExpiryTime() {
        return LocalDateTime.now().plusMinutes(OTP_VALIDITY_MINUTES)
                .atZone(java.time.ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli();
    }

    public static boolean isOTPValid(long expiryTime) {
        return System.currentTimeMillis() < expiryTime;
    }

    public static long getRemainingMinutes(long expiryTime) {
        long diff = expiryTime - System.currentTimeMillis();
        return diff > 0 ? diff / (60 * 1000) : 0;
    }
}

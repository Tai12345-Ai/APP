package com.fx.login.service;

import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

@Service
public class PasswordResetService {
    private Map<String, String> emailToCode = new HashMap<>();
    private final Random random = new SecureRandom();

    public String generateCode() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder sb = new StringBuilder(6);
        for (int i = 0; i < 6; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }

    public void storeCode(String email, String code) {
        emailToCode.put(email, code);
    }

    public boolean verifyCode(String email, String inputCode) {
        return inputCode.equals(emailToCode.get(email));
    }
}

package com.itranswarp.exchange.bean;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import com.itranswarp.exchange.util.HashUtil;

public record AuthToken(Long userId, long expiresAt) {

    public boolean isExpired() {
        return System.currentTimeMillis() > expiresAt();
    }

    public boolean isAboutToExpire() {
        return expiresAt() - System.currentTimeMillis() < 1800_000;
    }

    public AuthToken refresh() {
        return new AuthToken(this.userId(), System.currentTimeMillis() + 3600_000);
    }

    /**
     * hash = hmacSha256(userId : expiresAt, hmacKey)
     * 
     * secureString = userId : expiresAt : hash
     */
    public String toSecureString(String hmacKey) {
        String payload = userId() + ":" + expiresAt();
        String hash = HashUtil.hmacSha256(payload, hmacKey);
        String token = payload + ":" + hash;
        return Base64.getUrlEncoder().encodeToString(token.getBytes(StandardCharsets.UTF_8));
    }

    public static AuthToken fromSecureString(String b64token, String hmacKey) {
        String token = new String(Base64.getUrlDecoder().decode(b64token), StandardCharsets.UTF_8);
        String[] ss = token.split("\\:");
        if (ss.length != 3) {
            throw new IllegalArgumentException("Invalid token.");
        }
        String uid = ss[0];
        String expires = ss[1];
        String sig = ss[2];
        if (!sig.equals(HashUtil.hmacSha256(uid + ":" + expires, hmacKey))) {
            throw new IllegalArgumentException("Invalid token.");
        }
        return new AuthToken(Long.parseLong(uid), Long.parseLong(expires));
    }
}

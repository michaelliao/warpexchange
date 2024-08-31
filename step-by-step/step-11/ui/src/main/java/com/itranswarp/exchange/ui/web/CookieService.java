package com.itranswarp.exchange.ui.web;

import java.time.Duration;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import com.itranswarp.exchange.bean.AuthToken;
import com.itranswarp.exchange.util.HttpUtil;

@Component
public class CookieService {

    final Logger logger = LoggerFactory.getLogger(getClass());

    public final String SESSION_COOKIE = "_exsession_";

    @Value("#{exchangeConfiguration.hmacKey}")
    String hmacKey;

    @Value("#{exchangeConfiguration.sessionTimeout}")
    Duration sessionTimeout;

    public long getExpiresInSeconds() {
        return sessionTimeout.toSeconds();
    }

    @Nullable
    public AuthToken findSessionCookie(HttpServletRequest request) {
        Cookie[] cs = request.getCookies();
        if (cs == null) {
            return null;
        }
        for (Cookie c : cs) {
            if (SESSION_COOKIE.equals(c.getName())) {
                String cookieStr = c.getValue();
                AuthToken token = AuthToken.fromSecureString(cookieStr, this.hmacKey);
                return token.isExpired() ? null : token;
            }
        }
        return null;
    }

    public void deleteSessionCookie(HttpServletRequest request, HttpServletResponse response) {
        logger.info("delete session cookie...");
        Cookie cookie = new Cookie(SESSION_COOKIE, "-deleted-");
        cookie.setPath("/");
        cookie.setMaxAge(0);
        cookie.setHttpOnly(true);
        cookie.setSecure(HttpUtil.isSecure(request));
        String host = request.getServerName();
        if (host != null && host.startsWith("www.")) {
            // set cookie for domain "domain.com":
            String domain = host.substring(4);
            cookie.setDomain(domain);
        }
        response.addCookie(cookie);
    }

    public void setSessionCookie(HttpServletRequest request, HttpServletResponse response, AuthToken token) {
        String cookieStr = token.toSecureString(this.hmacKey);
        logger.info("[Cookie] set session cookie: " + cookieStr);
        Cookie cookie = new Cookie(SESSION_COOKIE, cookieStr);
        cookie.setPath("/");
        cookie.setMaxAge(3600);
        cookie.setHttpOnly(true);
        cookie.setSecure(HttpUtil.isSecure(request));
        String host = request.getServerName();
        if (host != null) {
            cookie.setDomain(host);
        }
        response.addCookie(cookie);
    }
}

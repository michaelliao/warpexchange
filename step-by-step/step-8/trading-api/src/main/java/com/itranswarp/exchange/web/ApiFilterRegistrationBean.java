package com.itranswarp.exchange.web;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.itranswarp.exchange.ApiError;
import com.itranswarp.exchange.ApiException;
import com.itranswarp.exchange.bean.AuthToken;
import com.itranswarp.exchange.ctx.UserContext;
import com.itranswarp.exchange.model.ui.UserProfileEntity;
import com.itranswarp.exchange.support.AbstractFilter;
import com.itranswarp.exchange.user.UserService;

@Component
public class ApiFilterRegistrationBean extends FilterRegistrationBean<Filter> {

    @Autowired
    UserService userService;

    @Autowired
    ObjectMapper objectMapper;

    @Value("#{exchangeConfiguration.hmacKey}")
    String hmacKey;

    @PostConstruct
    public void init() {
        ApiFilter filter = new ApiFilter();
        setFilter(filter);
        addUrlPatterns("/api/*");
        setName(filter.getClass().getSimpleName());
        setOrder(100);
    }

    class ApiFilter extends AbstractFilter {

        @Override
        public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain)
                throws IOException, ServletException {
            HttpServletRequest request = (HttpServletRequest) req;
            HttpServletResponse response = (HttpServletResponse) resp;
            String path = request.getRequestURI();
            logger.info("process api {} {}...", request.getMethod(), path);
            // set default encoding:
            request.setCharacterEncoding("UTF-8");
            response.setCharacterEncoding("UTF-8");
            // try parse user:
            Long userId = null;
            try {
                userId = parseUser(request);
            } catch (ApiException e) {
                sendErrorResponse(response, e);
                return;
            }
            if (userId == null) {
                chain.doFilter(request, response);
            } else {
                try (UserContext ctx = new UserContext(userId)) {
                    chain.doFilter(request, response);
                }
            }
        }

        Long parseUser(HttpServletRequest request) {
            // 尝试通过Authorization Header认证用户:
            String auth = request.getHeader("Authorization");
            if (auth != null) {
                return parseUserFromAuthorization(auth);
            }
            // 尝试通过API Key认证用户:
            String apiKey = request.getHeader("API-Key");
            String apiSignature = request.getHeader("API-Signature");
            if (apiKey != null && apiSignature != null) {
                return parseUserFromApiKey(apiKey, apiSignature, request);
            }
            return null;
        }

        Long parseUserFromAuthorization(String auth) {
            if (auth.startsWith("Basic ")) {
                String eap = new String(Base64.getDecoder().decode(auth.substring(6)), StandardCharsets.UTF_8);
                int pos = eap.indexOf(':');
                if (pos < 1) {
                    throw new ApiException(ApiError.AUTH_SIGNIN_FAILED, "Invalid email or password.");
                }
                String email = eap.substring(0, pos);
                String passwd = eap.substring(pos + 1);
                UserProfileEntity p = userService.signin(email, passwd);
                Long userId = p.userId;
                if (logger.isDebugEnabled()) {
                    logger.debug("parse from basic authorization: {}", userId);
                }
                return userId;
            }
            if (auth.startsWith("Bearer ")) {
                AuthToken token = AuthToken.fromSecureString(auth.substring(7), hmacKey);
                if (token.isExpired()) {
                    return null;
                }
                if (logger.isDebugEnabled()) {
                    logger.debug("parse from bearer authorization: {}", token.userId());
                }
                return token.userId();
            }
            throw new ApiException(ApiError.AUTH_SIGNIN_FAILED, "Invalid Authorization header.");
        }

        Long parseUserFromApiKey(String apiKey, String apiSignature, HttpServletRequest request) {
            // TODO: 验证API-Key, API-Secret并返回userId
            return null;
        }

        void sendErrorResponse(HttpServletResponse response, ApiException e) throws IOException {
            response.sendError(400);
            response.setContentType("application/json");
            PrintWriter pw = response.getWriter();
            pw.write(objectMapper.writeValueAsString(e.error));
            pw.flush();
        }
    }
}

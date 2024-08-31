package com.itranswarp.exchange.ui.web;

import java.io.IOException;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.stereotype.Component;

import com.itranswarp.exchange.bean.AuthToken;
import com.itranswarp.exchange.ctx.UserContext;
import com.itranswarp.exchange.support.AbstractFilter;
import com.itranswarp.exchange.user.UserService;

/**
 * UIFilter: try parse user from cookie.
 */
@Component
public class UIFilterRegistrationBean extends FilterRegistrationBean<Filter> {

    @Autowired
    UserService userService;

    @Autowired
    CookieService cookieService;

    @PostConstruct
    public void init() {
        UIFilter filter = new UIFilter();
        setFilter(filter);
        addUrlPatterns("/*");
        setName(filter.getClass().getSimpleName());
        setOrder(100);
    }

    class UIFilter extends AbstractFilter {

        @Override
        public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain)
                throws IOException, ServletException {
            HttpServletRequest request = (HttpServletRequest) req;
            HttpServletResponse response = (HttpServletResponse) resp;
            String path = request.getRequestURI();
            if (logger.isDebugEnabled()) {
                logger.debug("process {} {}...", request.getMethod(), path);
            }
            // set default encoding:
            request.setCharacterEncoding("UTF-8");
            response.setCharacterEncoding("UTF-8");
            response.setContentType("text/html;charset=UTF-8");
            // try parse user:
            AuthToken auth = cookieService.findSessionCookie(request);
            if (auth != null && auth.isAboutToExpire()) {
                logger.info("refresh session cookie...");
                cookieService.setSessionCookie(request, response, auth.refresh());
            }
            Long userId = auth == null ? null : auth.userId();
            if (logger.isDebugEnabled()) {
                logger.debug("parsed user {} from session cookie.", userId);
            }
            try (UserContext ctx = new UserContext(userId)) {
                chain.doFilter(request, response);
            }
        }
    }
}

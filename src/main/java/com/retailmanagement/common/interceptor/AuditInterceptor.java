// FILE 328: common/interceptor/AuditInterceptor.java
package com.retailmanagement.common.interceptor;

import com.retailmanagement.modules.auth.security.UserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Component
public class AuditInterceptor implements HandlerInterceptor {

    private static final ThreadLocal<Long> startTimeThreadLocal = new ThreadLocal<>();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        startTimeThreadLocal.set(System.currentTimeMillis());

        String user = getCurrentUser();
        String method = request.getMethod();
        String uri = request.getRequestURI();
        String query = request.getQueryString();
        String ip = request.getRemoteAddr();
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME);

        log.info("AUDIT - Time: {}, User: {}, IP: {}, Method: {}, URI: {}, Query: {}",
                timestamp, user, ip, method, uri, query != null ? query : "");

        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response,
                           Object handler, ModelAndView modelAndView) {
        // Nothing to do here
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        Long startTime = startTimeThreadLocal.get();
        long duration = System.currentTimeMillis() - startTime;

        String user = getCurrentUser();
        int status = response.getStatus();
        String method = request.getMethod();
        String uri = request.getRequestURI();
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME);

        log.info("AUDIT - Time: {}, User: {}, Method: {}, URI: {}, Status: {}, Duration: {}ms",
                timestamp, user, method, uri, status, duration);

        startTimeThreadLocal.remove();
    }

    private String getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal) {
            return ((UserPrincipal) authentication.getPrincipal()).getUsername();
        }
        return "ANONYMOUS";
    }
}
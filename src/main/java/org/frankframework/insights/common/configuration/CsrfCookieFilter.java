package org.frankframework.insights.common.configuration;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.filter.OncePerRequestFilter;

public class CsrfCookieFilter extends OncePerRequestFilter {

    private final boolean isSecure;

    public CsrfCookieFilter(boolean isSecure) {
        this.isSecure = isSecure;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        CsrfToken csrfToken = (CsrfToken) request.getAttribute(CsrfToken.class.getName());

        if (csrfToken != null) {
            response.setHeader("X-XSRF-TOKEN", csrfToken.getToken());

            Cookie cookie = new Cookie("XSRF-TOKEN", csrfToken.getToken());
            cookie.setPath("/");
            cookie.setSecure(isSecure);
            cookie.setHttpOnly(false);
            response.addCookie(cookie);
        }

        filterChain.doFilter(request, response);
    }
}

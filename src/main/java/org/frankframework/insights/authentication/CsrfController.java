package org.frankframework.insights.authentication;

import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/csrf")
public class CsrfController {

    /**
     * Endpoint to get CSRF token.
     * This ensures the XSRF-TOKEN cookie is set for the client.
     */
    @GetMapping
    public CsrfToken csrf(CsrfToken token) {
        return token;
    }
}

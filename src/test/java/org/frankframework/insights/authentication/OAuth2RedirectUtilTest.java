package org.frankframework.insights.authentication;

import static org.mockito.Mockito.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.web.RedirectStrategy;

@ExtendWith(MockitoExtension.class)
public class OAuth2RedirectUtilTest {

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private RedirectStrategy redirectStrategy;

    @Test
    public void redirectToOrigin_withRefererAndQueryParam_redirectsCorrectly() throws IOException {
        when(request.getHeader("Referer")).thenReturn("http://localhost:4200/dashboard");

        OAuth2RedirectUtil.redirectToOrigin(request, response, redirectStrategy, "login=success");

        verify(redirectStrategy).sendRedirect(request, response, "http://localhost:4200/?login=success");
    }

    @Test
    public void redirectToOrigin_withRefererWithoutQueryParam_redirectsWithoutQuery() throws IOException {
        when(request.getHeader("Referer")).thenReturn("http://localhost:4200/");

        OAuth2RedirectUtil.redirectToOrigin(request, response, redirectStrategy, null);

        verify(redirectStrategy).sendRedirect(request, response, "http://localhost:4200/");
    }

    @Test
    public void redirectToOrigin_withEmptyQueryParam_redirectsWithoutQuery() throws IOException {
        when(request.getHeader("Referer")).thenReturn("http://localhost:8080/");

        OAuth2RedirectUtil.redirectToOrigin(request, response, redirectStrategy, "");

        verify(redirectStrategy).sendRedirect(request, response, "http://localhost:8080/");
    }

    @Test
    public void redirectToOrigin_withHttpsUrl_preservesProtocol() throws IOException {
        when(request.getHeader("Referer")).thenReturn("https://insights.frankframework.org/issues");

        OAuth2RedirectUtil.redirectToOrigin(request, response, redirectStrategy, "param=value");

        verify(redirectStrategy).sendRedirect(request, response, "https://insights.frankframework.org/?param=value");
    }

    @Test
    public void redirectToOrigin_withDifferentPorts_extractsCorrectAuthority() throws IOException {
        when(request.getHeader("Referer")).thenReturn("http://localhost:3000/app");

        OAuth2RedirectUtil.redirectToOrigin(request, response, redirectStrategy, "test=1");

        verify(redirectStrategy).sendRedirect(request, response, "http://localhost:3000/?test=1");
    }

    @Test
    public void redirectToOrigin_withoutReferer_redirectsToRoot() throws IOException {
        when(request.getHeader("Referer")).thenReturn(null);

        OAuth2RedirectUtil.redirectToOrigin(request, response, redirectStrategy, "login=success");

        verify(redirectStrategy).sendRedirect(request, response, "/?login=success");
    }

    @Test
    public void redirectToOrigin_withEmptyReferer_redirectsToRoot() throws IOException {
        when(request.getHeader("Referer")).thenReturn("");

        OAuth2RedirectUtil.redirectToOrigin(request, response, redirectStrategy, "login=error");

        verify(redirectStrategy).sendRedirect(request, response, "/?login=error");
    }

    @Test
    public void redirectToOrigin_withInvalidUri_redirectsToRoot() throws IOException {
        when(request.getHeader("Referer")).thenReturn("not-a-valid-url");

        OAuth2RedirectUtil.redirectToOrigin(request, response, redirectStrategy, "fallback=true");

        verify(redirectStrategy).sendRedirect(request, response, "/?fallback=true");
    }

    @Test
    public void redirectToOrigin_withMalformedUri_handlesGracefully() throws IOException {
        when(request.getHeader("Referer")).thenReturn("http://");

        OAuth2RedirectUtil.redirectToOrigin(request, response, redirectStrategy, "error=true");

        verify(redirectStrategy).sendRedirect(request, response, "/?error=true");
    }

    @Test
    public void redirectToOrigin_withComplexPath_extractsOnlyOrigin() throws IOException {
        when(request.getHeader("Referer")).thenReturn("http://localhost:4200/app/dashboard?existing=param#anchor");

        OAuth2RedirectUtil.redirectToOrigin(request, response, redirectStrategy, "new=param");

        verify(redirectStrategy).sendRedirect(request, response, "http://localhost:4200/?new=param");
    }
}

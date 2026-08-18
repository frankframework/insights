package org.frankframework.insights;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.servlet.function.EntityResponse;
import org.springframework.web.servlet.function.HandlerFunction;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;

public class InsightsApplicationSpaRouterTest {

    private static final String FIXTURE_LOCATION = "spa-fixture/";
    private static final String INDEX = FIXTURE_LOCATION + "index.html";

    private final RouterFunction<ServerResponse> router = InsightsWebappApplication.spaRouter(FIXTURE_LOCATION);

    @ParameterizedTest(name = "/graph/{0}")
    @ValueSource(
            strings = {
                "10.2-nightly",
                "10.1.1-nightly",
                "9.2-nightly",
                "master-nightly",
                "nightly",
                "v9.0.0",
                "v7.7.1",
                "v10.2.0",
                "8.0",
                "10.2",
                "v10.2.0-RC1",
                "v10.2.0-SNAPSHOT",
                "v8.1.0-beta.2",
                "release-10.2"
            })
    public void releaseTag_isServedTheIndex(String tagName) {
        assertThat(servedResource("/graph/" + tagName)).contains(INDEX);
    }

    @Test
    public void releaseBranchNightly_isServedTheIndex_regressionForIssue712() {
        assertThat(servedResource("/graph/10.2-nightly")).contains(INDEX);
    }

    @Test
    public void releaseBranchNightly_withNightlyQueryParameter_isServedTheIndex() {
        assertThat(servedResource("/graph/10.2-nightly", "nightly=")).contains(INDEX);
    }

    @Test
    public void releaseTag_withQueryParameters_isServedTheIndex() {
        assertThat(servedResource("/graph/v9.0.0", "extended=2&range=%5B9.0%2C10.0%29&nightly="))
                .contains(INDEX);
    }

    @Test
    public void releaseTag_thatIsUrlEncoded_isServedTheIndex() {
        assertThat(servedResource("/graph/release%2F10.2")).contains(INDEX);
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(
            strings = {
                "/",
                "/graph",
                "/graph/",
                "/roadmap",
                "/cve-overview",
                "/cve-overview/CVE-2024-12345",
                "/vulnerabilities/manage",
                "/vulnerabilities/manage/CVE-2024-12345",
                "/release-manage/v9.0.0",
                "/release-manage/10.2-nightly",
                "/release-manage/10.2-nightly/business-values",
                "/release-manage/10.2-nightly/business-values/42",
                "/not-found",
                "/some/unknown/deep/route"
            })
    public void clientSideRoute_isServedTheIndex(String path) {
        assertThat(servedResource(path)).contains(INDEX);
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"/assets", "/assets/", "/assets/icons", "/media"})
    public void directory_isServedTheIndex(String path) {
        assertThat(servedResource(path)).contains(INDEX);
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(
            strings = {
                "/index.html",
                "/main-B7QM4KUC.js",
                "/polyfills-B6TNHZQ6.js",
                "/chunk-EWTVZH6O.mjs",
                "/styles-5INURTSO.css",
                "/main-B7QM4KUC.js.map",
                "/3rdpartylicenses.txt",
                "/favicon.ico",
                "/manifest.webmanifest",
                "/assets/favicon.svg",
                "/assets/icons/grab-gesture.png",
                "/media/inter-latin.woff2"
            })
    public void existingFile_isServedAsItself(String path) {
        assertThat(servedResource(path)).contains(FIXTURE_LOCATION + path.substring(1));
    }

    @Test
    public void existingFileWithoutExtension_isServedAsItself() {
        assertThat(servedResource("/robots")).contains(FIXTURE_LOCATION + "robots");
    }

    @Test
    public void existingFile_withQueryParameters_isServedAsItself() {
        assertThat(servedResource("/main-B7QM4KUC.js", "v=2")).contains(FIXTURE_LOCATION + "main-B7QM4KUC.js");
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"/does-not-exist.js", "/assets/missing.svg", "/styles-OUTDATED.css"})
    public void missingFile_fallsBackToTheIndex(String path) {
        assertThat(servedResource(path)).contains(INDEX);
    }

    @Test
    public void pathTraversal_fallsBackToTheIndex() {
        assertThat(servedResource("/assets/../../application.properties")).contains(INDEX);
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(
            strings = {
                "/api",
                "/api/releases",
                "/api/releases/10.2-nightly",
                "/api/vulnerabilities/release/10.2-nightly",
                "/api/auth/user",
                "/api/unknown-endpoint",
                "/error"
            })
    public void excludedPath_isNotRouted(String path) {
        assertThat(route(path)).isEmpty();
    }

    @Test
    public void routeStartingWithApiLetters_isServedTheIndex() {
        assertThat(servedResource("/apidocs")).contains(INDEX);
    }

    @Test
    public void routeNestedUnderError_isServedTheIndex() {
        assertThat(servedResource("/error/details")).contains(INDEX);
    }

    @Test
    public void headRequest_forClientSideRoute_isRouted() {
        assertThat(route("HEAD", "/graph/10.2-nightly", null)).isPresent();
    }

    @Test
    public void postRequest_forClientSideRoute_isRejectedAsMethodNotAllowed() {
        ServerResponse response = handle("POST", "/graph/10.2-nightly");

        assertThat(response.statusCode()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
    }

    @Test
    public void optionsRequest_forClientSideRoute_advertisesTheSupportedMethods() {
        ServerResponse response = handle("OPTIONS", "/graph/10.2-nightly");

        assertThat(response.statusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.headers().getAllow())
                .containsExactlyInAnyOrder(HttpMethod.GET, HttpMethod.HEAD, HttpMethod.OPTIONS);
    }

    @ParameterizedTest(name = "/graph/{0} (extension-like suffix \"{1}\")")
    @CsvSource({
        "10.2-nightly, 2-nightly",
        "10.1.1-nightly, 1-nightly",
        "v9.0.0, 0",
        "v10.2.0-RC1, 0-RC1",
        "v10.2.0-SNAPSHOT, 0-SNAPSHOT"
    })
    public void tagWithExtensionLikeSuffix_isServedTheIndex(String tagName, String extensionLikeSuffix) {
        assertThat(tagName).endsWith("." + extensionLikeSuffix);
        assertThat(servedResource("/graph/" + tagName)).contains(INDEX);
    }

    @Test
    public void applicationBean_fallsBackToTheAngularBuildOutput() {
        RouterFunction<ServerResponse> bean = new InsightsWebappApplication().spaRouter();
        ServerRequest request = request("GET", "/graph/10.2-nightly", null);

        ServerResponse response = handle(bean.route(request).orElseThrow(), request);
        Resource resource = (Resource) ((EntityResponse<?>) response).entity();

        assertThat(((ClassPathResource) resource).getPath()).isEqualTo("frontend/index.html");
    }

    private Optional<String> servedResource(String path) {
        return servedResource(path, null);
    }

    private Optional<String> servedResource(String path, String queryString) {
        ServerRequest request = request("GET", path, queryString);

        return router.route(request)
                .map(handler -> handle(handler, request))
                .map(response -> (Resource) ((EntityResponse<?>) response).entity())
                .map(resource -> ((ClassPathResource) resource).getPath());
    }

    private Optional<HandlerFunction<ServerResponse>> route(String path) {
        return route("GET", path, null);
    }

    private Optional<HandlerFunction<ServerResponse>> route(String method, String path, String queryString) {
        return router.route(request(method, path, queryString));
    }

    private ServerResponse handle(String method, String path) {
        ServerRequest request = request(method, path, null);
        HandlerFunction<ServerResponse> handler = router.route(request).orElseThrow();

        return handle(handler, request);
    }

    private ServerResponse handle(HandlerFunction<ServerResponse> handler, ServerRequest request) {
        try {
            return handler.handle(request);
        } catch (Exception e) {
            throw new IllegalStateException("Could not handle " + request.path(), e);
        }
    }

    private ServerRequest request(String method, String path, String queryString) {
        MockHttpServletRequest servletRequest = new MockHttpServletRequest(method, path);
        servletRequest.setQueryString(queryString);

        return ServerRequest.create(servletRequest, Collections.emptyList());
    }
}

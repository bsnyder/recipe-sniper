package com.recipesniper.service;

import io.opentelemetry.api.OpenTelemetry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScrapingServiceTest {

    @Mock
    private HttpClient httpClient;

    @Mock
    private HttpResponse<String> httpResponse;

    @TempDir
    Path tempDir;

    private ScrapingService scrapingService;

    @BeforeEach
    void setUp() {
        scrapingService = new ScrapingService(
                OpenTelemetry.noop().getTracer("test"),
                httpClient,
                tempDir);
    }

    @Test
    void shouldDownloadSaveAndParseHtml() throws Exception {
        String url = "https://example.com/recipe";
        String html = "<html><head><title>Test Recipe</title></head><body>content</body></html>";

        when(httpClient.send(any(HttpRequest.class), eq(HttpResponse.BodyHandlers.ofString())))
                .thenReturn(httpResponse);
        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn(html);

        ScrapingService.ScrapeResult result = scrapingService.scrape(url);

        assertThat(result.title()).isEqualTo("Test Recipe");
        assertThat(result.html()).contains("content");
        assertThat(result.savedFile()).exists();
        assertThat(result.savedFile().getParent()).isEqualTo(tempDir);
    }

    @Test
    void shouldThrowExceptionForInvalidUrl() {
        assertThatThrownBy(() -> scrapingService.scrape(""))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldThrowOnHttpError() throws Exception {
        String url = "https://example.com/bad";

        when(httpClient.send(any(HttpRequest.class), eq(HttpResponse.BodyHandlers.ofString())))
                .thenReturn(httpResponse);
        when(httpResponse.statusCode()).thenReturn(403);

        assertThatThrownBy(() -> scrapingService.scrape(url))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("403");
    }

    @Test
    void shouldThrowOnConnectionFailure() throws Exception {
        String url = "https://example.com/fail";

        when(httpClient.send(any(HttpRequest.class), eq(HttpResponse.BodyHandlers.ofString())))
                .thenThrow(new IOException("Connection refused"));

        assertThatThrownBy(() -> scrapingService.scrape(url))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("Connection refused");
    }
}

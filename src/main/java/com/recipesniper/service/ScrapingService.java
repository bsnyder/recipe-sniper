package com.recipesniper.service;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

@Service
public class ScrapingService {

    private static final Logger log = LoggerFactory.getLogger(ScrapingService.class);
    private static final String USER_AGENT =
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 "
                    + "(KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36";

    private final Tracer tracer;
    private final HttpClient httpClient;
    private final Path storageDir;

    @Autowired
    public ScrapingService(Tracer tracer,
                           @Value("${recipesniper.scrape.storage-dir:./data/pages}") String storageDir) {
        this.tracer = tracer;
        this.httpClient = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(Duration.ofSeconds(15))
                .build();
        this.storageDir = Path.of(storageDir);
    }

    // Visible for testing
    ScrapingService(Tracer tracer, HttpClient httpClient, Path storageDir) {
        this.tracer = tracer;
        this.httpClient = httpClient;
        this.storageDir = storageDir;
    }

    public record ScrapeResult(String title, String html, Path savedFile) {
    }

    public ScrapeResult scrape(String url) throws IOException {
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("URL must not be blank");
        }

        Span span = tracer.spanBuilder("ScrapingService.scrape")
                .setAttribute("recipe.url", url)
                .startSpan();

        try (Scope scope = span.makeCurrent()) {
            log.info("Downloading URL: {}", url);

            // Download the page using HttpClient with browser-like headers
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("User-Agent", USER_AGENT)
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                    .header("Accept-Language", "en-US,en;q=0.9")
                    .timeout(Duration.ofSeconds(30))
                    .GET()
                    .build();

            HttpResponse<String> response;
            try {
                response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("Download interrupted for URL: " + url, e);
            }

            if (response.statusCode() >= 400) {
                throw new IOException("HTTP " + response.statusCode() + " when fetching URL: " + url);
            }

            String rawHtml = response.body();

            // Save the HTML to a local file
            Files.createDirectories(storageDir);
            String safeName = url.replaceAll("[^a-zA-Z0-9.-]", "_");
            if (safeName.length() > 200) {
                safeName = safeName.substring(0, 200);
            }
            Path savedFile = storageDir.resolve(safeName + ".html");
            Files.writeString(savedFile, rawHtml);
            log.info("Saved page to: {}", savedFile);

            // Parse the saved file with Jsoup
            Document doc = Jsoup.parse(savedFile.toFile(), "UTF-8");
            String title = doc.title();
            String html = doc.html();

            span.setAttribute("recipe.title", title);
            span.setAttribute("recipe.savedFile", savedFile.toString());
            log.info("Successfully scraped: {}", title);

            return new ScrapeResult(title, html, savedFile);
        } catch (IOException e) {
            span.setStatus(StatusCode.ERROR, e.getMessage());
            span.recordException(e);
            throw e;
        } finally {
            span.end();
        }
    }
}

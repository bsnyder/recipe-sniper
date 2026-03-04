/*
 * Copyright 2026 Bruce Snyder (bsnyder@apache.org)
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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

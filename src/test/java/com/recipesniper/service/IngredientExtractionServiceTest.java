package com.recipesniper.service;

import com.recipesniper.service.IngredientExtractionService.ParsedIngredient;
import io.opentelemetry.api.trace.Tracer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.anyString;

@ExtendWith(MockitoExtension.class)
class IngredientExtractionServiceTest {

    @Mock
    private Tracer tracer;

    private IngredientExtractionService extractionService;

    @BeforeEach
    void setUp() {
        // Use noop tracer for tests
        extractionService = new IngredientExtractionService(
                io.opentelemetry.api.OpenTelemetry.noop().getTracer("test"));
    }

    @Test
    void shouldExtractIngredientsFromJsonLd() {
        String html = """
                <html><head>
                <script type="application/ld+json">
                {
                    "@context": "https://schema.org",
                    "@type": "Recipe",
                    "name": "Chocolate Cake",
                    "recipeIngredient": [
                        "2 cups all-purpose flour",
                        "1 cup sugar",
                        "3 large eggs"
                    ]
                }
                </script>
                </head><body></body></html>
                """;

        List<ParsedIngredient> ingredients = extractionService.extract(html);

        assertThat(ingredients).hasSize(3);
        assertThat(ingredients.get(0).rawText()).isEqualTo("2 cups all-purpose flour");
        assertThat(ingredients.get(1).rawText()).isEqualTo("1 cup sugar");
        assertThat(ingredients.get(2).rawText()).isEqualTo("3 large eggs");
    }

    @Test
    void shouldParseQuantityUnitAndName() {
        String html = """
                <html><head>
                <script type="application/ld+json">
                {
                    "@type": "Recipe",
                    "recipeIngredient": ["2 cups flour"]
                }
                </script>
                </head><body></body></html>
                """;

        List<ParsedIngredient> ingredients = extractionService.extract(html);

        assertThat(ingredients).hasSize(1);
        assertThat(ingredients.get(0).quantity()).isEqualTo("2");
        assertThat(ingredients.get(0).unit()).isEqualTo("cups");
        assertThat(ingredients.get(0).name()).isEqualTo("flour");
    }

    @Test
    void shouldHandleFractionalQuantities() {
        String html = """
                <html><head>
                <script type="application/ld+json">
                {
                    "@type": "Recipe",
                    "recipeIngredient": ["1/2 teaspoon salt"]
                }
                </script>
                </head><body></body></html>
                """;

        List<ParsedIngredient> ingredients = extractionService.extract(html);

        assertThat(ingredients).hasSize(1);
        assertThat(ingredients.get(0).quantity()).isEqualTo("1/2");
        assertThat(ingredients.get(0).unit()).isEqualTo("teaspoon");
        assertThat(ingredients.get(0).name()).isEqualTo("salt");
    }

    @Test
    void shouldHandleJsonLdWithGraphArray() {
        String html = """
                <html><head>
                <script type="application/ld+json">
                {
                    "@context": "https://schema.org",
                    "@graph": [
                        {
                            "@type": "Recipe",
                            "recipeIngredient": ["1 cup milk"]
                        }
                    ]
                }
                </script>
                </head><body></body></html>
                """;

        List<ParsedIngredient> ingredients = extractionService.extract(html);

        assertThat(ingredients).hasSize(1);
        assertThat(ingredients.get(0).rawText()).isEqualTo("1 cup milk");
    }

    @Test
    void shouldFallbackToHtmlParsing() {
        String html = """
                <html><body>
                <ul class="wprm-recipe-ingredients">
                    <li>2 cups flour</li>
                    <li>1 cup sugar</li>
                </ul>
                </body></html>
                """;

        List<ParsedIngredient> ingredients = extractionService.extract(html);

        assertThat(ingredients).hasSize(2);
    }

    @Test
    void shouldReturnEmptyListWhenNoIngredientsFound() {
        String html = "<html><body><p>No recipe here</p></body></html>";

        List<ParsedIngredient> ingredients = extractionService.extract(html);

        assertThat(ingredients).isEmpty();
    }

    @Test
    void shouldHandleIngredientWithNoUnit() {
        String html = """
                <html><head>
                <script type="application/ld+json">
                {
                    "@type": "Recipe",
                    "recipeIngredient": ["3 eggs"]
                }
                </script>
                </head><body></body></html>
                """;

        List<ParsedIngredient> ingredients = extractionService.extract(html);

        assertThat(ingredients).hasSize(1);
        assertThat(ingredients.get(0).quantity()).isEqualTo("3");
        assertThat(ingredients.get(0).name()).isEqualTo("eggs");
    }
}

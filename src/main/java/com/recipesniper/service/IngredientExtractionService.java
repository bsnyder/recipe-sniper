package com.recipesniper.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class IngredientExtractionService {

    private static final Logger log = LoggerFactory.getLogger(IngredientExtractionService.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private static final Set<String> UNITS = Set.of(
            "cup", "cups", "tablespoon", "tablespoons", "tbsp",
            "teaspoon", "teaspoons", "tsp", "ounce", "ounces", "oz",
            "pound", "pounds", "lb", "lbs", "gram", "grams", "g",
            "kilogram", "kilograms", "kg", "milliliter", "milliliters", "ml",
            "liter", "liters", "l", "pinch", "dash", "clove", "cloves",
            "slice", "slices", "piece", "pieces", "can", "cans",
            "package", "packages", "bunch", "bunches", "stick", "sticks",
            "quart", "quarts", "pint", "pints", "gallon", "gallons"
    );

    // Matches: "2", "1/2", "1 1/2", "0.5"
    private static final Pattern QUANTITY_PATTERN =
            Pattern.compile("^(\\d+\\s+\\d+/\\d+|\\d+/\\d+|\\d+\\.\\d+|\\d+)\\s*(.*)$");

    private final Tracer tracer;

    public IngredientExtractionService(Tracer tracer) {
        this.tracer = tracer;
    }

    public record ParsedIngredient(String name, String quantity, String unit, String rawText) {
    }

    public List<ParsedIngredient> extract(String html) {
        Span span = tracer.spanBuilder("IngredientExtractionService.extract").startSpan();

        try (Scope scope = span.makeCurrent()) {
            Document doc = Jsoup.parse(html);

            // Try JSON-LD first
            List<ParsedIngredient> ingredients = extractFromJsonLd(doc);
            if (!ingredients.isEmpty()) {
                span.setAttribute("extraction.method", "json-ld");
                span.setAttribute("extraction.count", ingredients.size());
                log.info("Extracted {} ingredients via JSON-LD", ingredients.size());
                return ingredients;
            }

            // Fallback to HTML parsing
            ingredients = extractFromHtml(doc);
            span.setAttribute("extraction.method", "html-fallback");
            span.setAttribute("extraction.count", ingredients.size());
            log.info("Extracted {} ingredients via HTML fallback", ingredients.size());
            return ingredients;
        } catch (Exception e) {
            span.setStatus(StatusCode.ERROR, e.getMessage());
            span.recordException(e);
            log.error("Failed to extract ingredients", e);
            return List.of();
        } finally {
            span.end();
        }
    }

    private List<ParsedIngredient> extractFromJsonLd(Document doc) {
        Elements scripts = doc.select("script[type=application/ld+json]");
        for (Element script : scripts) {
            try {
                JsonNode root = objectMapper.readTree(script.html());
                List<ParsedIngredient> ingredients = findRecipeIngredients(root);
                if (!ingredients.isEmpty()) {
                    return ingredients;
                }
            } catch (Exception e) {
                log.debug("Failed to parse JSON-LD block: {}", e.getMessage());
            }
        }
        return List.of();
    }

    private List<ParsedIngredient> findRecipeIngredients(JsonNode node) {
        // Direct Recipe type
        if (isRecipeNode(node)) {
            return parseIngredientArray(node.get("recipeIngredient"));
        }

        // Check @graph array
        JsonNode graph = node.get("@graph");
        if (graph != null && graph.isArray()) {
            for (JsonNode item : graph) {
                if (isRecipeNode(item)) {
                    return parseIngredientArray(item.get("recipeIngredient"));
                }
            }
        }

        // Check if root is an array
        if (node.isArray()) {
            for (JsonNode item : node) {
                if (isRecipeNode(item)) {
                    return parseIngredientArray(item.get("recipeIngredient"));
                }
            }
        }

        return List.of();
    }

    private boolean isRecipeNode(JsonNode node) {
        JsonNode type = node.get("@type");
        if (type == null) {
            return false;
        }
        if (type.isTextual()) {
            return "Recipe".equals(type.asText());
        }
        if (type.isArray()) {
            for (JsonNode t : type) {
                if ("Recipe".equals(t.asText())) {
                    return true;
                }
            }
        }
        return false;
    }

    private List<ParsedIngredient> parseIngredientArray(JsonNode ingredientNode) {
        if (ingredientNode == null || !ingredientNode.isArray()) {
            return List.of();
        }

        List<ParsedIngredient> result = new ArrayList<>();
        for (JsonNode item : ingredientNode) {
            String raw = item.asText().trim();
            if (!raw.isEmpty()) {
                result.add(parseIngredientString(raw));
            }
        }
        return result;
    }

    private List<ParsedIngredient> extractFromHtml(Document doc) {
        List<ParsedIngredient> ingredients = new ArrayList<>();

        // Common CSS selectors for ingredient lists
        String[] selectors = {
                ".wprm-recipe-ingredients li",
                ".recipe-ingredients li",
                ".ingredients li",
                "[class*=ingredient] li",
                "[itemprop=recipeIngredient]"
        };

        for (String selector : selectors) {
            Elements elements = doc.select(selector);
            if (!elements.isEmpty()) {
                for (Element el : elements) {
                    String text = el.text().trim();
                    if (!text.isEmpty()) {
                        ingredients.add(parseIngredientString(text));
                    }
                }
                return ingredients;
            }
        }

        return ingredients;
    }

    ParsedIngredient parseIngredientString(String raw) {
        Matcher matcher = QUANTITY_PATTERN.matcher(raw.trim());
        if (matcher.matches()) {
            String quantity = matcher.group(1).trim();
            String remainder = matcher.group(2).trim();

            // Check if first word of remainder is a unit
            String[] words = remainder.split("\\s+", 2);
            if (words.length >= 2 && UNITS.contains(words[0].toLowerCase())) {
                return new ParsedIngredient(words[1].trim(), quantity, words[0], raw);
            }
            // No unit recognized — the rest is the ingredient name
            return new ParsedIngredient(remainder, quantity, null, raw);
        }

        // No quantity found — entire string is the name
        return new ParsedIngredient(raw, null, null, raw);
    }
}

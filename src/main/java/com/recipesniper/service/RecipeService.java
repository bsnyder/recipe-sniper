package com.recipesniper.service;

import com.recipesniper.dto.IngredientResponse;
import com.recipesniper.dto.RecipeDetailResponse;
import com.recipesniper.dto.RecipeResponse;
import com.recipesniper.entity.Recipe;
import com.recipesniper.entity.RecipeIngredient;
import com.recipesniper.repository.RecipeRepository;
import com.recipesniper.service.IngredientExtractionService.ParsedIngredient;
import com.recipesniper.service.ScrapingService.ScrapeResult;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.List;

@Service
public class RecipeService {

    private static final Logger log = LoggerFactory.getLogger(RecipeService.class);

    private final RecipeRepository recipeRepository;
    private final ScrapingService scrapingService;
    private final IngredientExtractionService extractionService;
    private final Tracer tracer;

    public RecipeService(RecipeRepository recipeRepository,
                         ScrapingService scrapingService,
                         IngredientExtractionService extractionService,
                         Tracer tracer) {
        this.recipeRepository = recipeRepository;
        this.scrapingService = scrapingService;
        this.extractionService = extractionService;
        this.tracer = tracer;
    }

    @Transactional
    public RecipeDetailResponse addRecipe(String url) throws IOException {
        Span span = tracer.spanBuilder("RecipeService.addRecipe")
                .setAttribute("recipe.url", url)
                .startSpan();

        try (Scope scope = span.makeCurrent()) {
            log.info("Adding recipe from URL: {}", url);

            // Scrape
            ScrapeResult scrapeResult = scrapingService.scrape(url);

            // Extract ingredients
            List<ParsedIngredient> parsed = extractionService.extract(scrapeResult.html());

            // Build entity
            Recipe recipe = new Recipe();
            recipe.setUrl(url);
            recipe.setTitle(scrapeResult.title());
            recipe.setRawHtml(scrapeResult.html());

            for (ParsedIngredient pi : parsed) {
                RecipeIngredient ingredient = new RecipeIngredient();
                ingredient.setName(pi.name());
                ingredient.setQuantity(pi.quantity());
                ingredient.setUnit(pi.unit());
                ingredient.setRawText(pi.rawText());
                ingredient.setRecipe(recipe);
                recipe.getIngredients().add(ingredient);
            }

            Recipe saved = recipeRepository.save(recipe);
            span.setAttribute("recipe.id", saved.getId());
            span.setAttribute("recipe.ingredientCount", saved.getIngredients().size());
            log.info("Saved recipe '{}' with {} ingredients", saved.getTitle(), saved.getIngredients().size());

            return toDetailResponse(saved);
        } catch (Exception e) {
            span.setStatus(StatusCode.ERROR, e.getMessage());
            span.recordException(e);
            throw e;
        } finally {
            span.end();
        }
    }

    @Transactional(readOnly = true)
    public List<RecipeResponse> getAllRecipes() {
        return recipeRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<RecipeResponse> searchRecipes(String query) {
        return recipeRepository.findByTitleContainingIgnoreCase(query).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public RecipeDetailResponse getRecipeById(Long id) {
        Recipe recipe = recipeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Recipe not found: " + id));
        return toDetailResponse(recipe);
    }

    @Transactional
    public void deleteRecipe(Long id) {
        if (!recipeRepository.existsById(id)) {
            throw new IllegalArgumentException("Recipe not found: " + id);
        }
        recipeRepository.deleteById(id);
        log.info("Deleted recipe {}", id);
    }

    private RecipeResponse toResponse(Recipe recipe) {
        return new RecipeResponse(
                recipe.getId(),
                recipe.getUrl(),
                recipe.getTitle(),
                recipe.getIngredients().size(),
                recipe.getCreatedAt()
        );
    }

    private RecipeDetailResponse toDetailResponse(Recipe recipe) {
        List<IngredientResponse> ingredients = recipe.getIngredients().stream()
                .map(i -> new IngredientResponse(
                        i.getId(), i.getName(), i.getQuantity(), i.getUnit(), i.getRawText()))
                .toList();
        return new RecipeDetailResponse(
                recipe.getId(),
                recipe.getUrl(),
                recipe.getTitle(),
                recipe.getCreatedAt(),
                ingredients
        );
    }
}

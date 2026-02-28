# AGENTS.md

This file provides guidance to WARP (warp.dev) when working with code in this repository.

## Project Overview

Recipe Sniper is a recipe scraping and shopping list builder. Users paste a recipe URL, the backend scrapes and parses it (extracting structured ingredients), and those recipes can be combined into shopping lists with automatic ingredient deduplication and quantity merging.

## Build & Run Commands

This is a Spring Boot 4 (Java 21) + React (Vite/TypeScript) monorepo. There is no Maven wrapper; use a system-installed `mvn`.

### Backend

- **Full build (backend + frontend):** `mvn clean package`
- **Build backend only (skip frontend):** `mvn clean package -Dskip.frontend=true`
- **Run the app:** `mvn spring-boot:run` (serves on port 8080)
- **Run all tests:** `mvn test`
- **Run a single test class:** `mvn test -Dtest=ScrapingServiceTest`
- **Run a single test method:** `mvn test -Dtest=ScrapingServiceTest#shouldDownloadSaveAndParseHtml`
- **Skip tests during build:** `mvn clean package -DskipTests`
- **Docker:** `docker compose up --build` (builds and runs on port 8080)

### Frontend

All commands run from `frontend/`:

- **Install deps:** `npm install`
- **Dev server:** `npm run dev` (port 5173, proxies `/api` to backend on 8080)
- **Production build:** `npm run build` (outputs to `frontend/dist/`, copied into Spring Boot static resources during `mvn package`)
- **Type check:** `npx tsc --noEmit`

## Architecture

### Backend (Spring Boot 4 / Java 21)

Package: `com.recipesniper`

**Request flow:** Controller → Service → Repository (Spring Data JPA / H2)

Key services and their responsibilities:
- `ScrapingService` — Downloads a recipe URL via `java.net.http.HttpClient`, saves raw HTML to `./data/pages/`, and returns a parsed Jsoup `Document`. Uses a package-private constructor accepting an `HttpClient` for test mocking.
- `IngredientExtractionService` — Extracts ingredients using a two-strategy approach: first tries Schema.org JSON-LD (`<script type="application/ld+json">` with `@type: Recipe`), then falls back to HTML CSS selectors targeting common recipe plugin classes (e.g. `.wprm-recipe-ingredients`). Parses raw ingredient strings into structured `(quantity, unit, name)` via regex.
- `RecipeService` — Orchestrates scraping + extraction, builds `Recipe` entities with `RecipeIngredient` children.
- `ShoppingListService` — Creates shopping lists from recipes, merges duplicate ingredients by name (case-insensitive), sums quantities when units match, concatenates when they differ.

**Data model:**
- `Recipe` ←1:N→ `RecipeIngredient` (cascade all, orphan removal)
- `ShoppingList` ←M:N→ `Recipe` (join table `shopping_list_recipe`)
- `ShoppingList` ←1:N→ `ShoppingListItem` (cascade all, orphan removal)
- H2 file-based DB at `./data/recipe-sniper` (in-memory `jdbc:h2:mem:testdb` for tests with `create-drop`)

**DTOs** are Java records in the `dto` package. Request DTOs use Jakarta Validation annotations. The `GlobalExceptionHandler` maps `IllegalArgumentException` → 404, `MethodArgumentNotValidException` → 400, `IOException` → 502.

**Observability:** All services create manual OpenTelemetry spans with attributes. The `OpenTelemetryConfig` provides a named `Tracer` bean. In tests, use `OpenTelemetry.noop().getTracer("test")`.

### Frontend (React 19 / TypeScript / Vite)

- `frontend/src/api/client.ts` — Typed fetch wrapper for all REST endpoints
- `frontend/src/types/index.ts` — TypeScript interfaces mirroring backend DTOs
- `frontend/src/pages/` — Page components using React Router v7
- No state management library; pages fetch data directly via `client.ts`

### REST API

- `POST /api/recipes` — Scrape and store a recipe from URL
- `GET /api/recipes[?search=]` — List/search recipes
- `GET /api/recipes/{id}` — Recipe detail with ingredients
- `DELETE /api/recipes/{id}`
- `POST /api/shopping-lists` — Create from recipe IDs
- `GET /api/shopping-lists` / `GET /api/shopping-lists/{id}`
- `PUT /api/shopping-lists/{id}` — Full replace of name + items
- `POST /api/shopping-lists/{id}/recipes` — Add recipes to existing list
- `DELETE /api/shopping-lists/{id}`

## Testing Conventions

- Tests use JUnit 5 + Mockito + AssertJ
- Controller tests use `@WebMvcTest` with `@MockitoBean` for service dependencies
- Repository tests use `@DataJpaTest`
- Service tests use `@ExtendWith(MockitoExtension.class)` with manual construction (no Spring context)
- Test config in `src/test/resources/application.yml` uses in-memory H2

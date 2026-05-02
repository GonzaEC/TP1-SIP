# TP1 SIP — Selenium WebDriver Scraper Multi-Browser — G-ONE

Trabajo práctico de la materia **Seminario de Integración Profesional (SIP)**.  
Scraper multi-browser de MercadoLibre Argentina construido de forma incremental con **Java 17** y **Selenium WebDriver 4**.

---

## Integrantes

| Nombre                        | Legajo |
|-------------------------------|--------|
| Roberto Soto                  | 156302 |
| Nicolas Romero                | 195347 |
| Cristian Tomás Anito          | 158887 |
| Rocco Buzzo Marcelo           | 190292 |
| Gonzalo Echeverria Crenna     | 195155 |
| Federico Matias Claros Garcia | 166717 |

---

## Descripción general

El objetivo del TP es construir, hit a hit, un scraper que busque productos en MercadoLibre AR, aplique filtros y extraiga resultados de forma estructurada. Cada hit agrega funcionalidad sobre el anterior.

El sitio elegido presenta los desafíos clásicos del scraping moderno: contenido renderizado por JavaScript, selectores que cambian entre versiones, banners que interceptan clicks, lazy loading y diferencias sutiles entre navegadores.

**Productos objetivo:**
- Bicicleta rodado 29
- iPhone 16 Pro Max
- GeForce RTX 5090

---

## Stack tecnológico

| Tecnología        | Versión                    | Rol                                          |
|-------------------|----------------------------|----------------------------------------------|
| Java              | 17                         | Lenguaje principal                           |
| Selenium WebDriver| 4.20.0                     | Automatización del navegador                 |
| Selenium Manager  | (incluido en Selenium 4)   | Descarga automática de chromedriver/geckodriver |
| Maven             | 3.x                        | Build y gestión de dependencias              |
| Chrome / Firefox  | stable                     | Navegadores objetivo                         |
| JUnit 5           | 5.10.2                     | Framework de testing                         |
| Mockito           | 5.11.0                     | Mocking en tests unitarios                   |
| JaCoCo            | 0.8.12                     | Cobertura de código (mínimo 70 %)            |
| Docker            | multi-stage                | Empaquetado reproducible con browsers incluidos |
| GitHub Actions    | —                          | Pipeline de CI/CD                            |
| pre-commit        | —                          | Hooks locales (gitleaks, checkstyle, spotless)|

---

## Estructura del repositorio

```
TP1-SIP/
├── .github/
│   └── workflows/
│       └── ci.yml                  ← Pipeline CI (unit tests, docker, e2e, gitleaks)
├── .pre-commit-config.yaml         ← Hooks locales pre-commit
├── HIT1/   → Scraper básico con Chrome
├── HIT2/   → Browser Factory (Chrome y Firefox)
├── HIT3/   → Filtros por DOM + Screenshot
├── HIT4/   → Extracción multi-producto y JSON
├── HIT5/   → Robustez, reintentos y módulo Selectors
├── HIT6/
│   ├── checkstyle.xml              ← Reglas de estilo Java
│   ├── Dockerfile                  ← Multi-stage: builder + runtime con Chrome y Firefox
│   ├── docker-compose.yml          ← Servicios: scraper, lint, test
│   ├── docker-entrypoint.sh        ← Manejo de --browser y HEADLESS al iniciar el contenedor
│   ├── pom.xml
│   └── src/
│       ├── main/java/ar/edu/sip/
│       │   ├── BrowserFactory.java
│       │   ├── MercadoLibreScraper.java
│       │   ├── ProductResult.java
│       │   └── Selectors.java
│       └── test/java/ar/edu/sip/
│           ├── BrowserFactoryTest.java
│           ├── MercadoLibreScrapperTest.java
│           ├── ProductResultSchemaTest.java
│           └── ScrapperE2ETest.java
├── HIT7/   → (próximamente)
└── HIT8/   → (próximamente)
```

---

## Requisitos previos

- Java 17 o superior
- Maven 3.6 o superior
- Docker Desktop (o Docker Engine en Linux)
- Google Chrome y/o Mozilla Firefox instalados (solo para tests E2E locales)
- Python 3.8+ y `pip` (solo para activar pre-commit hooks)
- Conexión a internet (Selenium Manager descarga los drivers la primera vez)

---

## Hits implementados

> [!NOTE]
> Antes de pushear aplicar: `mvn spotless:apply` y `mvn clean validate`

### HIT 1 — Scraper básico con Chrome
**Carpeta:** `HIT1/`

Abre Chrome, navega a MercadoLibre AR, busca **"bicicleta rodado 29"** y muestra los títulos de los primeros 5 resultados. Toda la sincronización usa `WebDriverWait` + `ExpectedConditions`. Prohibido `Thread.sleep()`.

```bash
cd HIT1
mvn compile exec:java
```

---

### HIT 2 — Browser Factory
**Carpeta:** `HIT2/`

Refactorización del HIT1 que introduce una clase `BrowserFactory`. Recibe el nombre del navegador (`chrome` o `firefox`) y devuelve una instancia de `WebDriver` correctamente configurada. El navegador se elige por system property o variable de entorno sin tocar el código.

```bash
cd HIT2
mvn compile exec:java                     # Chrome (default)
mvn compile exec:java -Dbrowser=firefox   # Firefox
```

Cadena de resolución del navegador:
```
argumento directo → -Dbrowser → $BROWSER → "chrome"
```

---

### HIT 3 — Filtros por DOM y Screenshot
**Carpeta:** `HIT3/`

Aplica tres filtros sobre la página de resultados interactuando con el DOM (clicks reales, no modificación de URL):

- **Condición:** Nuevo
- **Tienda:** Solo tiendas oficiales
- **Orden:** Más relevantes

Además captura un screenshot de la página filtrada y lo guarda en `HIT3/screenshots/<producto>_<browser>.png`.

```bash
cd HIT3
mvn compile exec:java
mvn compile exec:java -Dbrowser=firefox
```

---

### HIT 4 — Extracción multi-producto y JSON
**Carpeta:** `HIT4/`

Generaliza el scraper para procesar los tres productos objetivo en una misma ejecución y guardar los resultados en archivos JSON estructurados.

**Campos extraídos por ítem:** `titulo`, `precio` (Long ARS), `link`, `tienda_oficial`, `envio_gratis`, `cuotas_sin_interes`.

```bash
cd HIT4
mvn compile exec:java                     # Chrome (default)
mvn compile exec:java -Dbrowser=firefox   # Firefox
```

Los JSONs se generan en `HIT4/output/<producto>.json` y los screenshots en `HIT4/screenshots/`.

---

### HIT 5 — Robustez, reintentos y módulo Selectors
**Carpeta:** `HIT5/`

Incorpora manejo de errores granular y un módulo centralizado de selectores.

- **`Selectors.java`**: todos los selectores CSS/XPath en una sola clase; actualizarlos ante cambios de layout de MercadoLibre no requiere tocar la lógica de negocio.
- **Reintentos automáticos**: hasta 3 intentos por producto ante cualquier excepción, sin `Thread.sleep()`.
- **Helpers `tryGetText` / `tryGetLong`**: campos opcionales retornan `null` en vez de propagar `NoSuchElementException`.

```bash
cd HIT5
mvn compile exec:java                     # Chrome (default)
mvn compile exec:java -Dbrowser=firefox   # Firefox
```

---

### HIT 6 — Headless, Tests Automatizados, Docker y CI
**Carpeta:** `HIT6/`

#### Modo headless

El modo headless se controla por variable de entorno o system property sin tocar el código:

```bash
cd HIT6

# Modo headless (sin abrir ventana)
HEADLESS=true mvn exec:java

# Modo visible (útil para debug)
HEADLESS=false mvn exec:java
```

Cadena de resolución:
```
-Dheadless → $HEADLESS → false (default)
```

---

#### Tests unitarios (sin browser)

Usan Mockito para simular `WebDriver` y `WebElement`. No abren ningún browser. Validan la lógica de `extraerDatos`, `tryGetText`, `tryGetLong` y `sanitizar`.

```bash
cd HIT6
mvn test
```

Cobertura mínima configurada: **70 %** (JaCoCo falla el build si cae debajo).  
El reporte HTML se genera en `HIT6/target/site/jacoco/index.html`.

```bash
# Generar reporte de cobertura
cd HIT6
mvn verify

# Abrir reporte (Linux)
xdg-open target/site/jacoco/index.html

# Abrir reporte (macOS)
open target/site/jacoco/index.html
```

---

#### Tests E2E (browser real contra mercadolibre.com.ar)

Requieren `INTEGRATION=true`. Levantan un Chrome real, navegan a MercadoLibre y validan los 4 criterios del hit contra datos reales.

```bash
cd HIT6

# Chrome headless
INTEGRATION=true HEADLESS=true mvn test

# Firefox headless
INTEGRATION=true HEADLESS=true BROWSER=firefox mvn test

# Chrome visible (debug)
INTEGRATION=true HEADLESS=false mvn test
```

---

#### Docker

La imagen es multi-stage: el stage `builder` compila con Maven y el stage `runtime` incluye JRE 17, Chrome, ChromeDriver, Firefox ESR y GeckoDriver con versiones fijas (sin `:latest`).

**Construir la imagen:**
```bash
cd HIT6
docker build -t ml-scraper:latest .
```

**Correr el scraper con Docker:**
```bash
# Chrome (default)
docker run --rm \
  -v $(pwd)/output:/app/output \
  -v $(pwd)/screenshots:/app/screenshots \
  ml-scraper:latest

# Firefox
docker run --rm \
  -v $(pwd)/output:/app/output \
  -v $(pwd)/screenshots:/app/screenshots \
  ml-scraper:latest --browser firefox

# Modo visible (requiere display virtual en Linux)
docker run --rm \
  -e HEADLESS=false \
  -v $(pwd)/output:/app/output \
  ml-scraper:latest
```

**Con Docker Compose:**
```bash
cd HIT6

# Chrome headless (default)
docker compose up scraper

# Firefox
BROWSER=firefox docker compose up scraper

# Lint (Checkstyle + Spotless, sin instalar nada local)
docker compose run --rm lint

# Tests unitarios + cobertura dentro del contenedor
docker compose run --rm test
```

Los JSONs generados quedan en `HIT6/output/` y los screenshots en `HIT6/screenshots/`, montados desde el host.

---

#### Pipeline CI (GitHub Actions)

El workflow `.github/workflows/ci.yml` corre automáticamente en cada push y pull request. La secuencia de jobs es:

```
secrets-scan
    └── unit-tests (chrome) ──┐
    └── unit-tests (firefox) ─┼── docker-scraper (chrome)
                               └── docker-scraper (firefox)
                               └── e2e-tests (chrome)    ← solo en main/master
                               └── e2e-tests (firefox)   ← solo en main/master
```

| Job | Qué hace | Cuándo corre |
|---|---|---|
| `secrets-scan` | Gitleaks sobre todo el historial | Siempre |
| `unit-tests` | `mvn verify` + JaCoCo ≥ 70 % en matriz chrome/firefox | Siempre |
| `docker-scraper` | `docker build` + `docker run` headless en matriz chrome/firefox | Si unit-tests pasa |
| `e2e-tests` | Tests E2E con browser real en matriz chrome/firefox | Solo en `main`/`master` |

**Artifacts publicados por el pipeline:**
- `jacoco-report-{browser}` — reporte HTML de cobertura (14 días)
- `surefire-results-{browser}` — XML de resultados de tests (7 días)
- `output-json-docker-{browser}` — JSONs generados por el scraper en Docker (7 días)
- `screenshots-docker-{browser}` — screenshots del scraper en Docker (7 días)
- `output-json-e2e-{browser}` — JSONs generados por los tests E2E (7 días)
- `screenshots-e2e-{browser}` — screenshots ante fallo E2E (7 días)

---

#### Pre-commit hooks locales

Los hooks se ejecutan automáticamente antes de cada `git commit` y bloquean el push si detectan problemas. Requieren Python.

**Instalación (una sola vez por clon):**
```bash
pip install pre-commit
pre-commit install
```

**Hooks configurados:**

| Hook | Qué valida |
|---|---|
| `gitleaks` | Secrets hardcodeados (tokens, passwords, keys) |
| `trailing-whitespace` | Espacios al final de línea |
| `end-of-file-fixer` | Newline al final de cada archivo |
| `check-merge-conflict` | Markers de merge sin resolver |
| `check-yaml` / `check-xml` | Sintaxis de YAML y XML |
| `spotless-check` | Formato Java (Google Java Format) |
| `checkstyle-check` | Estilo Java (nombres, llaves, imports) |

**Ejecución manual sobre todos los archivos:**
```bash
pre-commit run --all-files
```

**Saltear hooks puntualmente (no recomendado):**
```bash
git commit --no-verify -m "mensaje"
```

---

## Principios de implementación

### Explicit waits — sin Thread.sleep()

Toda la sincronización usa `WebDriverWait` con condiciones específicas:

| Condición | Uso |
|---|---|
| `elementToBeClickable` | Antes de escribir en campos o hacer click en botones |
| `presenceOfElementLocated` | Confirmar que los resultados cargaron en el DOM |
| `visibilityOfElementLocated` | Esperar que elementos ocultos se vuelvan visibles |
| `invisibilityOfElementLocated` | Confirmar que el banner de cookies desapareció |

### Selectores con fallback

MercadoLibre cambia su HTML con frecuencia. Los selectores de títulos se definen como una lista ordenada; el código prueba cada uno hasta encontrar el primero que devuelva elementos con texto:

```java
private static final String[] SELECTORES_TITULO = {
    "a.poly-component__title",       // layout poly (2024-2025)
    "h2.poly-box a",
    ".ui-search-item__title",
    "li.ui-search-layout__item h2 a",
    "a.ui-search-link__title-card"
};
```

### JS click para overlays

Cuando un overlay (banner de cookies, header sticky, dropdown abierto) tapa el elemento objetivo, Selenium lanza `ElementClickInterceptedException` con el click nativo. Se resuelve con:

```java
((JavascriptExecutor) driver).executeScript("arguments[0].click();", elemento);
```

### Selenium Manager

Selenium 4 incluye Selenium Manager, un binario que detecta la versión del navegador instalado y descarga el driver correcto (`chromedriver`, `geckodriver`) automáticamente. No se necesita configuración manual ni dependencias adicionales de driver.

---

## Diferencias entre Chrome y Firefox

| Aspecto | Chrome | Firefox |
|---|---|---|
| Protocolo de inspección | CDP (Chrome DevTools Protocol) | WebDriver BiDi |
| Warning de versión | Sí (`Unable to find CDP matching 147`) | No |
| Clase de opciones | `ChromeOptions` | `FirefoxOptions` |
| Suprimir detección WebDriver | `--disable-blink-features=AutomationControlled` | `dom.webdriver.enabled = false` |
| Selectores CSS en resultados | Idénticos (mismo HTML del servidor) | Idénticos |
| Tiempo de arranque | Más rápido | Más lento (primer uso descarga geckodriver) |
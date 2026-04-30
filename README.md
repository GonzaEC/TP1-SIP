# TP1 SIP — Selenium WebDriver Scraper Multi-Browser — G-ONE

Trabajo práctico de la materia **Seminario de Integracion Profesional(SIP)**.  
Scraper multi-browser de MercadoLibre Argentina construido de forma incremental con **Java 17** y **Selenium WebDriver 4**.

---
## Integrantes

| Nombre                    | Legajo |
|---------------------------|---|
| Roberto Soto              | 156302 |
| Nicolas Romero            | 195347 |
| Cristian Tomás Anito      | 158887 |
| Rocco Buzzo Marcelo       | 190292 |
| Gonzalo Echeverria Crenna | 195155 |
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

| Tecnología | Versión | Rol |
|---|---|---|
| Java | 17 | Lenguaje principal |
| Selenium WebDriver | 4.20.0 | Automatización del navegador |
| Selenium Manager | (incluido en Selenium 4) | Descarga automática de chromedriver / geckodriver |
| Maven | 3.x | Build y gestión de dependencias |
| Chrome / Firefox | latest | Navegadores objetivo |

---

## Estructura del repositorio

```
TP1-SIP/
├── HIT1/   → Scraper básico con Chrome
├── HIT2/   → Browser Factory (Chrome y Firefox)
├── HIT3/   → Filtros por DOM + Screenshot
├── HIT4/   → (próximamente)
├── HIT5/   → (próximamente)
├── HIT6/   → (próximamente)
├── HIT7/   → (próximamente)
└── HIT8/   → (próximamente)
```

Cada carpeta es un proyecto Maven independiente con su propio `pom.xml` y `README.md`.

---

## Hits implementados

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

---

## Requisitos

- Java 17 o superior
- Maven 3.6 o superior
- Google Chrome y/o Mozilla Firefox instalados
- Conexión a internet (Selenium Manager descarga los drivers la primera vez)

// HIT6/src/test/java/ar/edu/sip/ScrapperE2ETest.java
package ar.edu.sip;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.Keys;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.net.URI;
import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests E2E que lanzan un WebDriver real contra mercadolibre.com.ar.
 *
 * Se ejecutan SOLO cuando la variable de entorno INTEGRATION=true.
 * En CI se activan en el job de integración (ver workflow).
 *
 * Requieren: ChromeDriver o GeckoDriver instalado y en PATH.
 * Usan HEADLESS=true y BROWSER según las variables de entorno del runner.
 */
@EnabledIfEnvironmentVariable(named = "INTEGRATION", matches = "true")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ScraperE2ETest
{

    private static WebDriver driver;
    private static WebDriverWait wait;
    private static final String PRODUCTO_TEST = "iPhone 16 Pro Max";

	@BeforeAll
	static void setUp() throws Exception
	{
		// Usa BROWSER + HEADLESS del entorno
		driver = BrowserFactory.create(null);
		wait   = new WebDriverWait(driver, Duration.ofSeconds(20));

		// Solo navegar y buscar, sin guardar JSON ni screenshot
		driver.get("https://www.mercadolibre.com.ar");
		WebElement campo = wait.until(
			ExpectedConditions.elementToBeClickable(Selectors.INPUT_BUSQUEDA)
		);
		campo.clear();
		campo.sendKeys(PRODUCTO_TEST, Keys.ENTER);
		MercadoLibreScraper.esperarResultados(wait, PRODUCTO_TEST);
	}

	@AfterAll
	static void tearDown() { if (driver != null) driver.quit(); }

	// ── 1. Al menos 10 resultados ────────────────────────────────────────────

	@Test
	@Order(1)
	void e2e_scraper_extraeAlMenos10Resultados()
	{
		List<ProductResult> resultados =
			MercadoLibreScraper.extraerDatos(driver, PRODUCTO_TEST);

		assertTrue(
			resultados.size() >= 10,
			"Se esperaban al menos 10 resultados, se obtuvieron: " +
			resultados.size()
		);
	}

    // ── 2. Schema mínimo ─────────────────────────────────────────────────────

    @Test
    @Order(2)
    void e2e_todos_los_items_tienenTituloYLink() {
        List<ProductResult> resultados =
            MercadoLibreScraper.extraerDatos(driver, PRODUCTO_TEST);

        assertFalse(resultados.isEmpty(), "La lista no debe estar vacía");

        resultados.forEach(p -> {
            assertNotNull(p.getTitulo(), "titulo null en: " + p.getLink());
            assertFalse(p.getTitulo().isBlank(), "titulo vacío en: " + p.getLink());
            assertNotNull(p.getLink(), "link null");
        });
    }

    // ── 3. Precios positivos ─────────────────────────────────────────────────

    @Test
    @Order(3)
    void e2e_precios_sonPositivos() {
        List<ProductResult> resultados =
            MercadoLibreScraper.extraerDatos(driver, PRODUCTO_TEST);

        resultados.stream()
            .filter(p -> p.getPrecio() != null)
            .forEach(p -> assertTrue(p.getPrecio() > 0,
                "Precio no positivo: " + p.getPrecio() + " en " + p.getTitulo()));
    }

    // ── 4. Links absolutos ───────────────────────────────────────────────────

    @Test
    @Order(4)
    void e2e_links_sonUrlsAbsolutasHTTPS() {
        List<ProductResult> resultados =
            MercadoLibreScraper.extraerDatos(driver, PRODUCTO_TEST);

        resultados.stream()
            .filter(p -> p.getLink() != null)
            .forEach(p -> {
                URI uri = assertDoesNotThrow(() -> new URI(p.getLink()),
                    "link inválido: " + p.getLink());
                assertTrue(uri.isAbsolute(), "link no absoluto: " + p.getLink());
                assertTrue(uri.getScheme().startsWith("http"),
                    "scheme inesperado: " + p.getLink());
            });
    }
}
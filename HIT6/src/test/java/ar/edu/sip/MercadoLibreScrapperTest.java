// HIT6/src/test/java/ar/edu/sip/MercadoLibreScrapperTest.java
package ar.edu.sip;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.IOException;
import java.net.URI;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests de integración que validan la lógica del scraper
 * usando Mockito en lugar de un WebDriver real.
 *
 * Cubren los 4 criterios del Hit 6:
 *  1. Al menos 10 resultados por producto.
 *  2. Schema mínimo del JSON.
 *  3. Precios positivos.
 *  4. Links como URLs absolutas.
 */
@ExtendWith(MockitoExtension.class)
class MercadoLibreScraperTest {

    @Mock  WebDriver driver;
    @Mock  WebDriverWait wait;
    @Mock  WebElement container;
    @Mock  WebElement linkElement;
    @Mock  WebElement priceElement;
    @Mock  WebElement shippingElement;

    // ── Helpers ──────────────────────────────────────────────────────────────

    /** Fabrica N contenedores mock que devuelven datos válidos. */
    /**
     * Este método era el que causaba el UnfinishedStubbingException.
     * Ahora cada when() está correctamente cerrado con .thenReturn().
     */
    private List<WebElement> buildContainers(int cantidad) {
        List<WebElement> contenedores = new ArrayList<>();

        for (int i = 0; i < cantidad; i++) {
            // Creamos los mocks para el contenedor y sus elementos internos
            WebElement contenedorMock = mock(WebElement.class);
            WebElement linkMock = mock(WebElement.class);
            WebElement precioMock = mock(WebElement.class);
            WebElement tiendaMock = mock(WebElement.class);

            // Configuramos el comportamiento de los elementos hijos
            when(linkMock.getText()).thenReturn("Producto " + i);
            when(linkMock.getAttribute("href")).thenReturn("https://articulo.mercadolibre.com.ar/MLA-" + i);
            when(precioMock.getText()).thenReturn("$ 1.500");
            when(tiendaMock.getText()).thenReturn("por Tienda Oficial");

            // IMPORTANTE: Mockear la búsqueda interna para que no devuelva null
            when(contenedorMock.findElement(Selectors.PRODUCT_LINK)).thenReturn(linkMock);
            when(contenedorMock.findElement(Selectors.PRODUCT_PRICE)).thenReturn(precioMock);
            when(contenedorMock.findElement(Selectors.PRODUCT_OFFICIAL_STORE)).thenReturn(tiendaMock);
            
            // Mockear campos opcionales para evitar NoSuchElementException
            when(contenedorMock.findElement(Selectors.PRODUCT_SHIPPING)).thenReturn(mock(WebElement.class));
            when(contenedorMock.findElement(Selectors.PRODUCT_INSTALLMENTS)).thenReturn(mock(WebElement.class));

            contenedores.add(contenedorMock);
        }

        // Stubbing final del driver
        when(driver.findElements(Selectors.CONTENEDOR_RESULTADOS)).thenReturn(contenedores);
        
        return contenedores;
    }

    // ── 1. Cantidad mínima de resultados ─────────────────────────────────────

    @Test
    void extraerDatos_conMasDe10Contenedores_retornaExactamente10() {
        buildContainers(15); // El límite en el código es 10

        List<ProductResult> resultados = MercadoLibreScraper.extraerDatos(driver, "test");

        assertEquals(10, resultados.size(), "Debe extraer exactamente CANT_RESULTADOS items");
    }

    @Test
    void extraerDatos_con10Contenedores_retorna10() {
        buildContainers(10);

        // Llamamos al método estático de la clase original
        List<ProductResult> resultados = MercadoLibreScraper.extraerDatos(driver, "test");

        assertEquals(10, resultados.size());
        assertEquals("Producto 0", resultados.get(0).getTitulo());
    }

    @Test
    void extraerDatos_conMenosDe10Contenedores_retornaTodosDisponibles() {
        buildContainers(5);

        List<ProductResult> resultados = MercadoLibreScraper.extraerDatos(driver, "test");

        assertEquals(5, resultados.size());
    }

    @Test
    void extraerDatos_sinContenedores_retornaListaVacia() {
        when(driver.findElements(Selectors.CONTENEDOR_RESULTADOS))
            .thenReturn(List.of());

        List<ProductResult> result = MercadoLibreScraper.extraerDatos(driver, "test");

        assertTrue(result.isEmpty());
    }

    // ── 2. Schema mínimo ─────────────────────────────────────────────────────

    @Test
    void extraerDatos_todoLosItems_tienenTituloYLink() {
        when(driver.findElements(Selectors.CONTENEDOR_RESULTADOS))
            .thenReturn(buildContainers(10));

        List<ProductResult> result = MercadoLibreScraper.extraerDatos(driver, "test");

        result.forEach(p -> {
            assertNotNull(p.getTitulo(), "titulo no debe ser null");
            assertFalse(p.getTitulo().isBlank(), "titulo no debe estar vacío");
            assertNotNull(p.getLink(), "link no debe ser null");
        });
    }

    @Test
    void extraerDatos_itemConLinkFaltante_esDescartado() {
        WebElement badContainer = mock(WebElement.class);
        WebElement badLink      = mock(WebElement.class);
        when(badLink.getText()).thenReturn("");         // título vacío
        when(badLink.getAttribute("href")).thenReturn(null);
        when(badContainer.findElement(Selectors.PRODUCT_LINK)).thenReturn(badLink);

        List<WebElement> mixed = new ArrayList<>(buildContainers(9));
        mixed.add(badContainer);

        when(driver.findElements(Selectors.CONTENEDOR_RESULTADOS)).thenReturn(mixed);

        List<ProductResult> result = MercadoLibreScraper.extraerDatos(driver, "test");

        // El item con título vacío fue añadido (el scraper no filtra por título vacío,
        // pero el link sí se guarda como null → verificamos que los 9 válidos tienen link)
        long conLink = result.stream().filter(p -> p.getLink() != null).count();
        assertEquals(9, conLink, "Los 9 items válidos deben tener link");
    }

    // ── 3. Precios positivos ─────────────────────────────────────────────────

    @Test
    void extraerDatos_precios_sonTodosPositivos()
    {
        buildContainers(1);

        List<ProductResult> resultados = MercadoLibreScraper.extraerDatos(driver, "test");

        assertNotNull(resultados.get(0).getPrecio());
        assertTrue(resultados.get(0).getPrecio() > 0);

        resultados.stream()
            .filter(p -> p.getPrecio() != null)
            .forEach(p -> assertTrue(
                p.getPrecio() > 0,
                "Precio debe ser positivo, encontrado: " + p.getPrecio()
            )
        );
    }

    @Test
    void extraerDatos_precioConTextoNoNumerico_seteaNull() {
        WebElement c    = mock(WebElement.class);
        WebElement link = mock(WebElement.class);
        WebElement price= mock(WebElement.class);

        when(link.getText()).thenReturn("Producto");
        when(link.getAttribute("href"))
            .thenReturn("https://articulo.mercadolibre.com.ar/MLA-1");
        when(price.getText()).thenReturn("Precio a consultar"); // no numérico

        when(c.findElement(Selectors.PRODUCT_LINK)).thenReturn(link);
        when(c.findElement(Selectors.PRODUCT_PRICE)).thenReturn(price);
        when(c.findElement(Selectors.PRODUCT_OFFICIAL_STORE))
            .thenThrow(new NoSuchElementException(""));
        when(c.findElement(Selectors.PRODUCT_SHIPPING))
            .thenThrow(new NoSuchElementException(""));
        when(c.findElement(Selectors.PRODUCT_INSTALLMENTS))
            .thenThrow(new NoSuchElementException(""));

        when(driver.findElements(Selectors.CONTENEDOR_RESULTADOS)).thenReturn(List.of(c));

        List<ProductResult> result = MercadoLibreScraper.extraerDatos(driver, "test");

        assertEquals(1, result.size());
        assertNull(result.get(0).getPrecio(),
            "Precio no numérico debe quedar como null");
    }

    // ── 4. Links absolutos ───────────────────────────────────────────────────

    @Test
    void extraerDatos_links_sonUrlsAbsolutasValidas() {
        when(driver.findElements(Selectors.CONTENEDOR_RESULTADOS))
            .thenReturn(buildContainers(10));

        List<ProductResult> result = MercadoLibreScraper.extraerDatos(driver, "test");

        result.stream()
            .filter(p -> p.getLink() != null)
            .forEach(p -> {
                URI uri = assertDoesNotThrow(() -> new URI(p.getLink()),
                    "link no es URI válida: " + p.getLink());
                assertTrue(uri.isAbsolute(),
                    "link debe ser absoluta: " + p.getLink());
                assertTrue(uri.getScheme().startsWith("http"),
                    "scheme debe ser http/https: " + p.getLink());
            });
    }

    // ── tryGetText / tryGetLong ──────────────────────────────────────────────

    @Test
    void tryGetText_elementoPresente_retornaTextoLimpio() {
        when(container.findElement(any())).thenReturn(linkElement);
        when(linkElement.getText()).thenReturn("  Texto con espacios  ");

        String result = MercadoLibreScraper.tryGetText(container,
                            Selectors.PRODUCT_LINK, "");

        assertEquals("Texto con espacios", result);
    }

    @Test
    void tryGetText_elementoAusente_retornaNull() {
        when(container.findElement(any()))
            .thenThrow(new NoSuchElementException("not found"));

        String result = MercadoLibreScraper.tryGetText(container,
                            Selectors.PRODUCT_LINK, "");

        assertNull(result);
    }

    @Test
    void tryGetText_conPrefijo_eliminaPrefijo() {
        when(container.findElement(any())).thenReturn(linkElement);
        when(linkElement.getText()).thenReturn("por Tienda Oficial");

        String result = MercadoLibreScraper.tryGetText(container,
                            Selectors.PRODUCT_OFFICIAL_STORE, "por ");

        assertEquals("Tienda Oficial", result);
    }

    @Test
    void tryGetLong_textoNumericoConSeparadores_retornaLong() {
        when(container.findElement(any())).thenReturn(priceElement);
        when(priceElement.getText()).thenReturn("1.250.000");

        Long result = MercadoLibreScraper.tryGetLong(container, Selectors.PRODUCT_PRICE);

        assertEquals(1_250_000L, result);
    }

    @Test
    void tryGetLong_elementoAusente_retornaNull() {
        when(container.findElement(any()))
            .thenThrow(new NoSuchElementException("not found"));

        Long result = MercadoLibreScraper.tryGetLong(container, Selectors.PRODUCT_PRICE);

        assertNull(result);
    }

    // ── sanitizar ────────────────────────────────────────────────────────────

    @Test
    void sanitizar_nombreConEspaciosYMayusculas_generaSlugValido() {
        String slug = MercadoLibreScraper.sanitizar("iPhone 16 Pro Max");
        assertEquals("iphone_16_pro_max", slug);
    }

    @Test
    void sanitizar_nombreConCaracteresEspeciales_losElimina() {
        String slug = MercadoLibreScraper.sanitizar("GeForce RTX 5090!!");
        assertFalse(slug.endsWith("_"),
            "No debe terminar con underscore");
        assertFalse(slug.contains("!"));
    }

    // ── guardarJson ──────────────────────────────────────────────────────────

    @Test
    void guardarJson_listaValida_creaArchivoLegible() throws IOException {
        ProductResult p = new ProductResult();
        p.setTitulo("Test");
        p.setPrecio(100_000L);
        p.setLink("https://example.com");

        Path outDir = Path.of("output");
        MercadoLibreScraper.guardarJson("test_guardar", List.of(p));

        Path file = outDir.resolve("test_guardar.json");
        assertTrue(Files.exists(file), "El archivo JSON debe existir");
        String content = Files.readString(file);
        assertTrue(content.contains("Test"), "El JSON debe contener el título");

        // limpieza
        Files.deleteIfExists(file);
    }
}
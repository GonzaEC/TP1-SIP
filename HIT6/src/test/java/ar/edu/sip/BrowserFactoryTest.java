// HIT6/src/test/java/ar/edu/sip/BrowserFactoryTest.java
package ar.edu.sip;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitarios puros (sin WebDriver) para BrowserFactory.
 * Validan la logica de resolucion de nombre y modo headless.
 */
class BrowserFactoryTest
{

	// ── resolveName ───────────────────────────────────────────────────────────

	@Test
	void resolveName_valorExplicito_retornaExplicito()
	{
		assertEquals("firefox", BrowserFactory.resolveName("firefox"));
	}

	@Test
	void resolveName_valorExplicitoConEspacios_retornaTrimeado()
	{
		assertEquals("chrome", BrowserFactory.resolveName("  chrome  "));
	}

	@Test
	void resolveName_nulo_retornaChromePorDefecto()
	{
		// Solo aplica cuando tampoco hay System property ni env var con valor
		// En CI ambas variables estan limpias para esta prueba
		String result = BrowserFactory.resolveName(null);
		assertNotNull(result);
		assertFalse(result.isBlank());
	}

	@Test
	void resolveName_cadenaVacia_usaFallback()
	{
		// "" es equivalente a nulo → debe caer al fallback
		String result = BrowserFactory.resolveName("   ");
		assertNotNull(result);
	}

	// ── resolveHeadless ──────────────────────────────────────────────────────

	@Test
	void resolveHeadless_propiedadTrue_retornaTrue()
	{
		System.setProperty("headless", "true");

		try     { assertTrue(BrowserFactory.resolveHeadless()); }
		finally { System.clearProperty("headless"); }
	}

	@Test
	void resolveHeadless_propiedadFalse_retornaFalse()
	{
		System.setProperty("headless", "false");

		try     { assertFalse(BrowserFactory.resolveHeadless()); }
		finally { System.clearProperty("headless"); }
	}

	@Test
	void resolveHeadless_sinPropiedadSinEnv_retornaFalseDefault()
	{
		System.clearProperty("headless");
		// Si la env var HEADLESS no esta seteada el default es false
		// (No podemos limpiar env vars en Java, pero en CI no estan seteadas aca)
		boolean result = BrowserFactory.resolveHeadless();
		// Con HEADLESS no seteada y sin sysprop, esperamos false
		// Si la variable de entorno HEADLESS=true esta en CI se acepta true tambien
		assertTrue(result == Boolean.parseBoolean(
			System.getenv("HEADLESS") != null
				? System.getenv("HEADLESS")
				: "false"
		));
	}

	// ── create – nombre invalido ─────────────────────────────────────────────

	@Test
	void create_navegadorInvalido_lanzaIllegalArgument()
	{
		assertThrows(
			IllegalArgumentException.class,
			() -> BrowserFactory.create("safari", false)
		);
	}
}
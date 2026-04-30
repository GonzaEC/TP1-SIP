// HIT6/src/main/java/ar/edu/sip/BrowserFactory.java
package ar.edu.sip;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;

import java.util.List;

public class BrowserFactory
{

	private BrowserFactory() {}
	// Definicion explicita de user agents para prevenir error en Mercado Libre
	private static String userAgent = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";

	public static WebDriver create(String browserName)
	{
		return create(browserName, resolveHeadless());
	}

	public static WebDriver create(String browserName, boolean headless)
	{
		String nombre = resolveName(browserName);
		System.out.println(
			"[BrowserFactory] Browser: " + nombre + " | Headless: " + headless
		);
		return switch (nombre.toLowerCase())
		{
			case "chrome"  -> buildChrome(headless);
			case "firefox" -> buildFirefox(headless);
			default -> throw new IllegalArgumentException(
				"Browser not supported: \""
				+ nombre +
				"\". Valid values: chrome, firefox"
			);
		};
	}

	public static WebDriver create() { return create(null); }

	public static String resolveName(String explicit)
	{
		if (explicit != null && !explicit.isBlank()) return explicit.trim();

		String prop = System.getProperty("browser");

		if (prop != null && !prop.isBlank()) return prop.trim();

		String env = System.getenv("BROWSER");

		if (env != null && !env.isBlank()) return env.trim();
		return "chrome";
	}
	/**
	 * Resuelve el modo headless.
	 * Prioridad: System property "headless" → variable de entorno HEADLESS → false por defecto.
	 */
	public static boolean resolveHeadless()
	{
		String property = System.getProperty("headless");

		if (property != null) return Boolean.parseBoolean(property.trim());

		String env = System.getenv("HEADLESS");
		if (env != null) return Boolean.parseBoolean(env.trim());

		return false;
	}

	private static WebDriver buildChrome(boolean headless)
	{
		ChromeOptions opts = new ChromeOptions();

		// Mantiene las configuraciones de automatización existentes
		opts.setExperimentalOption("excludeSwitches", List.of("enable-automation"));
		opts.addArguments("--disable-blink-features=AutomationControlled");

		// Workaround para evitar empty-state en MercadoLibre
		opts.addArguments("--user-agent=" + userAgent);

		if (headless) opts.addArguments(
			"--headless=new",
			"--no-sandbox",
			"--disable-dev-shm-usage",
			"--window-size=1920,1080"
		);
		return new ChromeDriver(opts);
	}

	private static WebDriver buildFirefox(boolean headless)
	{
		FirefoxOptions opts = new FirefoxOptions();

		opts.addPreference("dom.webdriver.enabled", false);
		opts.addPreference("useAutomationExtension", false);

		// Workaround para evitar empty-state en MercadoLibre
		opts.addArguments("--user-agent=" + userAgent);

		if (headless) opts.addArguments(
			"--headless",
			"--width=1920",
			"--height=1080"
		);
		return new FirefoxDriver(opts);
	}
}
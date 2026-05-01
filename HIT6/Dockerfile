# ─────────────────────────────────────────────────────────────────────────────
# STAGE 1 — builder
#   Compila el proyecto y empaqueta el fat-jar con todas las dependencias.
#   Usa Maven oficial con JDK 17 (pin de versión, sin :latest).
# ─────────────────────────────────────────────────────────────────────────────
FROM maven:3.9.6-eclipse-temurin-17 AS builder

WORKDIR /build

# Cachear dependencias antes de copiar el código fuente.
# Si pom.xml no cambia, Docker reutiliza esta capa.
COPY pom.xml .
RUN mvn dependency:go-offline -q

# Copiar fuentes y compilar (se saltean los tests: se corren en CI)
COPY src ./src
RUN mvn package -DskipTests -q

# ─────────────────────────────────────────────────────────────────────────────
# STAGE 2 — runtime
#   Imagen mínima con JRE 17 + Chrome + Firefox + sus drivers.
#   Pin: eclipse-temurin:17.0.11_9-jre-jammy  (Ubuntu 22.04 LTS)
# ─────────────────────────────────────────────────────────────────────────────
FROM eclipse-temurin:17.0.11_9-jre-jammy AS runtime

# ── Variables de configuración por defecto ───────────────────────────────────
ENV BROWSER=chrome \
    HEADLESS=true \
    DISPLAY=:99

WORKDIR /app

# ── Instalar dependencias del sistema ────────────────────────────────────────
RUN apt-get update -qq && \
    apt-get install -y --no-install-recommends \
        # Xvfb por si se necesita modo no-headless en el futuro
        xvfb \
        # Utilidades de red
        curl \
        wget \
        unzip \
        ca-certificates \
        fonts-liberation \
        libasound2 \
        libatk-bridge2.0-0 \
        libatk1.0-0 \
        libcups2 \
        libdbus-1-3 \
        libgdk-pixbuf2.0-0 \
        libnspr4 \
        libnss3 \
        libx11-6 \
        libxcomposite1 \
        libxdamage1 \
        libxext6 \
        libxfixes3 \
        libxrandr2 \
        libxss1 \
        libxtst6 \
        xdg-utils \
    && rm -rf /var/lib/apt/lists/*

# ── Instalar Google Chrome (versión estable fija) ────────────────────────────
RUN CHROME_VERSION="124.0.6367.91-1" && \
    wget -q "https://dl.google.com/linux/chrome/deb/pool/main/g/google-chrome-stable/google-chrome-stable_${CHROME_VERSION}_amd64.deb" \
        -O /tmp/chrome.deb && \
    apt-get install -y /tmp/chrome.deb && \
    rm /tmp/chrome.deb && \
    rm -rf /var/lib/apt/lists/*

# ── Instalar ChromeDriver (versión compatible con Chrome 124) ────────────────
RUN CHROMEDRIVER_VERSION="124.0.6367.91" && \
    wget -q "https://storage.googleapis.com/chrome-for-testing-public/${CHROMEDRIVER_VERSION}/linux64/chromedriver-linux64.zip" \
        -O /tmp/chromedriver.zip && \
    unzip -q /tmp/chromedriver.zip -d /tmp && \
    mv /tmp/chromedriver-linux64/chromedriver /usr/local/bin/chromedriver && \
    chmod +x /usr/local/bin/chromedriver && \
    rm -rf /tmp/chromedriver.zip /tmp/chromedriver-linux64

# ── Instalar Firefox ESR (versión LTS estable) ───────────────────────────────
RUN apt-get update -qq && \
    apt-get install -y --no-install-recommends firefox-esr && \
    rm -rf /var/lib/apt/lists/*

# ── Instalar GeckoDriver (para Firefox) ─────────────────────────────────────
RUN GECKODRIVER_VERSION="0.34.0" && \
    wget -q "https://github.com/mozilla/geckodriver/releases/download/v${GECKODRIVER_VERSION}/geckodriver-v${GECKODRIVER_VERSION}-linux64.tar.gz" \
        -O /tmp/geckodriver.tar.gz && \
    tar -xzf /tmp/geckodriver.tar.gz -C /usr/local/bin && \
    chmod +x /usr/local/bin/geckodriver && \
    rm /tmp/geckodriver.tar.gz

# ── Copiar el jar generado en el stage builder ───────────────────────────────
COPY --from=builder /build/target/hit5-scraper-1.0-SNAPSHOT.jar /app/scraper.jar

# ── Directorios de salida (montados desde el host) ───────────────────────────
RUN mkdir -p /app/output /app/screenshots

# ── Script de entrada ────────────────────────────────────────────────────────
COPY docker-entrypoint.sh /app/docker-entrypoint.sh
RUN chmod +x /app/docker-entrypoint.sh

VOLUME ["/app/output", "/app/screenshots"]

ENTRYPOINT ["/app/docker-entrypoint.sh"]
# Agent Context — TP1-SIP

> Archivo para que cualquier agente que retome este proyecto sepa rápidamente en qué estado estamos.

---

## Estado general

Proyecto del TP1 de Seminario de Integración Profesional (SIP).
Scraper multi-browser de MercadoLibre Argentina con Java 17 + Selenium.

**Hits completos:** 1, 2, 3, 4, 5, 6, 7 (en progreso — infra k8s armada, falta probar en cluster real)
**Hit pendiente:** 8 (aún no definido en la consigna)

---

## Lo que hicimos en la última sesión

### CI/GitHub Actions (problema principal de la sesión)
- **Problema:** Mercado Libre detecta las IPs de los runners de GitHub Actions (Azure) como bot y redirige a `/gz/account-verification`. El scraper fallaba en CI.
- **Fix aplicado:**
  - Renombramos `.github/workflows/ci.yml` → `scrape.yml` (como pide la consigna).
  - Agregamos `continue-on-error: true` al job `docker-scraper` para que el CI no falle completo cuando ML bloquea.
  - Capturamos el exit code manualmente y reportamos el estado (`success`/`failed`).
  - Agregamos un job `k8s-validate` que verifica sintaxis de los manifiestos Kubernetes con `kubectl apply --dry-run=client -f HIT7/k8s/`.

### BrowserFactory.java — fixes anti-detección
- **Chrome:** agregados flags extra (`--disable-gpu`, `--disable-extensions`, `--no-first-run`).
- **Firefox:** corregido bug crítico. `--user-agent` como argumento de CLI no funciona en geckodriver. Cambiado a preferencia `general.useragent.override`.
- Archivo: `HIT6/src/main/java/ar/edu/sip/BrowserFactory.java`

### MercadoLibreScraper.java — soporte para ConfigMap
- Agregado método `resolveProductos()` que lee `PRODUCTS` desde variable de entorno (multilínea). Si no existe, usa los 3 productos por defecto.
- Esto permite que el ConfigMap de Kubernetes defina la lista de productos.
- Archivo: `HIT6/src/main/java/ar/edu/sip/MercadoLibreScraper.java`

### HIT 7 — Kubernetes
- Creada carpeta `HIT7/k8s/` con 4 manifiestos:
  - `configmap.yaml` — `BROWSER`, `HEADLESS`, `LOG_LEVEL`, `PRODUCTS`
  - `pvc.yaml` — 1 GB, `storageClassName: local-path` (k3s out-of-the-box)
  - `job.yaml` — Job one-off
  - `cronjob.yaml` — CronJob `0 * * * *`
- Creada guía paso a paso para Windows: `HIT7/GUIA-K8S-WINDOWS.md`
- Actualizado `HIT7/README.md` con recetario completo.
- **Nota:** los manifiestos están en `HIT7/k8s/` (no en raíz). El CI apunta a `HIT7/k8s/`.

### ADRs
- Ya tenemos 5 ADRs (la consigna pide mínimo 4):
  - `0001-framework-automatizacion.md` (menú #1)
  - `0002-estrategia-selectores.md` (menú #4)
  - `0003-stack-java-maven.md` (propio)
  - `0004-pre-commit-vs-ci.md` (menú #6)
  - `0005-orquestacion-kubernetes.md` (propio)
- Todos en formato Michael Nygard (Contexto · Decisión · Consecuencias).

---

## Problemas conocidos

1. **Mercado Libre bloquea scraping desde IPs de datacenter.**
   - En CI (GitHub Actions/Azure) el scraper termina en `account-verification`.
   - El job `docker-scraper` tiene `continue-on-error: true` para no romper el pipeline.
   - Localmente (IP residencial) debería funcionar bien.

2. **Warning de CDP en Chrome:**
   - `Unable to find CDP implementation matching 147` — es cosmético, no rompe nada.

---

## Tests y cobertura

- `mvn verify` pasa OK en `HIT6/`.
- 63 tests, 0 fallos.
- JaCoCo ≥ 70% cumplido (actualmente ~80%).

---

## Qué falta / próximos pasos

1. **Probar HIT 7 en un cluster k3s/k3d real.** El usuario todavía no tiene k8s instalado. Ver guía en `HIT7/GUIA-K8S-WINDOWS.md`.
2. **Hit 8:** aún no está definido en la consigna (pendiente).
3. **Entrega final:** capturas de `kubectl get jobs` y `kubectl get cronjobs` para mostrar a la cátedra.

---

## Estructura clave del repo

```
TP1-SIP/
├── .github/workflows/scrape.yml          ← CI (gitleaks, tests, docker, k8s validate)
├── .pre-commit-config.yaml               ← Hooks locales (gitleaks, checkstyle, spotless)
├── HIT6/
│   ├── Dockerfile                         ← Multi-stage (builder + runtime con Chrome/Firefox)
│   ├── docker-compose.yml                 ← Servicios: scraper, lint, test
│   ├── docker-entrypoint.sh
│   ├── pom.xml
│   └── src/main/java/ar/edu/sip/
│       ├── BrowserFactory.java            ← Fix user-agent Firefox + flags anti-bot Chrome
│       ├── MercadoLibreScraper.java       ← resolveProductos() lee env PRODUCTS
│       ├── ProductResult.java
│       └── Selectors.java
├── HIT7/
│   ├── README.md                          ← Doc del hit 7
│   ├── GUIA-K8S-WINDOWS.md              ← Guía paso a paso para levantar k3d en Windows
│   └── k8s/
│       ├── configmap.yaml
│       ├── pvc.yaml
│       ├── job.yaml
│       └── cronjob.yaml
└── docs/adr/
    ├── 0000-template.md
    ├── 0001-framework-automatizacion.md
    ├── 0002-estrategia-selectores.md
    ├── 0003-stack-java-maven.md
    ├── 0004-pre-commit-vs-ci.md
    └── 0005-orquestacion-kubernetes.md
```

---

## Cómo verificar que todo compila

```bash
cd HIT6
mvn --batch-mode verify
```

Debería dar `BUILD SUCCESS` con cobertura JaCoCo ≥ 70%.

---

## Notas para el agente que retome

- El usuario está en **Windows** y es **principiante en Kubernetes**.
- No tiene k3s/k3d instalado todavía.
- La guía `HIT7/GUIA-K8S-WINDOWS.md` está pensada para que la siga paso a paso.
- Si el usuario pregunta "¿cómo hago k8s?", apuntalo a la guía.
- Si aparece `ImagePullBackOff` en los pods, casi seguro es porque no cargó la imagen con `k3d image import`.

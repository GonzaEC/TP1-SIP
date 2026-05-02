# TP1 SIP — HIT 7: Orquestación con Kubernetes (k3s)

Despliegue del scraper del [HIT 6](../HIT6/README.md) en un cluster Kubernetes (k3s/k3d) usando recursos nativos de batch: Job, CronJob, ConfigMap y PersistentVolumeClaim.

---

## Pre-requisitos

1. **Tener un cluster k3s o k3d funcional.**
   - **Opción A — k3s nativo (Linux / WSL2):**
     ```bash
     curl -sfL https://get.k3s.io | sh -
     sudo k3s kubectl get nodes
     ```
   - **Opción B — k3d (cualquier SO con Docker):**
     ```bash
     # Instalar k3d (ej: Windows con chocolatey)
     choco install k3d

     # Crear cluster
     k3d cluster create scraper

     # Verificar
     kubectl get nodes
     ```

2. **Tener la imagen Docker `ml-scraper:latest` construida.**
   ```bash
   cd HIT6
   docker build -t ml-scraper:latest .
   ```

3. **Tener `kubectl` instalado** (viene con k3s, o se instala por separado).

---

## Estructura de manifiestos (`HIT7/k8s/`)

| Archivo | Qué hace |
|---|---|
| `configmap.yaml` | Variables de entorno: `BROWSER`, `HEADLESS`, `LOG_LEVEL`, lista de `PRODUCTS` |
| `pvc.yaml` | Solicita 1 GB de almacenamiento persistente (`local-path`) |
| `job.yaml` | Ejecuta el scraper **una vez** y guarda resultados en el PVC |
| `cronjob.yaml` | Ejecuta el scraper **cada hora** (`0 * * * *`) con histórico |

---

## Recetario de ejecución

### 1. Cargar la imagen en el cluster

**Si usás k3s nativo:**
```bash
docker save ml-scraper:latest -o ml-scraper.tar
sudo k3s ctr images import ml-scraper.tar
rm ml-scraper.tar
```

**Si usás k3d:**
```bash
k3d image import ml-scraper:latest -c scraper
```

### 2. Aplicar todos los manifiestos

```bash
kubectl apply -f HIT7/k8s/
```

Esto crea el ConfigMap, el PVC, el Job y el CronJob.

### 3. Verificar que todo se creó

```bash
kubectl get configmap scraper-config
kubectl get pvc scraper-output
kubectl get jobs
kubectl get cronjobs
```

### 4. Disparar / observar el Job one-off

```bash
# Ver pods del Job
kubectl get pods -l job-name=scraper-once

# Seguir logs en tiempo real
kubectl logs -l job-name=scraper-once -f
```

### 5. Verificar que los JSONs quedaron en el PVC

```bash
# Encontrar el Pod que corrió el Job
POD_NAME=$(kubectl get pod -l job-name=scraper-once -o jsonpath='{.items[0].metadata.name}')

# Listar archivos generados
kubectl exec -it $POD_NAME -- ls -la /app/output
kubectl exec -it $POD_NAME -- ls -la /app/screenshots
```

### 6. Verificar el CronJob

```bash
# Ver estado del cron
kubectl get cronjob scraper-hourly

# Ver jobs creados por el cron (aparecen cada hora)
kubectl get jobs --watch
```

---

## Limpieza

```bash
# Borrar todos los recursos creados
kubectl delete -f HIT7/k8s/
```

---

## Notas

- El scraper lee la lista de productos desde la variable de entorno `PRODUCTS` (multilínea). Si no está definida, usa los 3 productos por defecto.
- El PVC usa `storageClassName: local-path` que viene pre-instalado en k3s. No hace falta crear ningún StorageClass manualmente.
- Ambos Job y CronJob comparten el mismo PVC, por lo que los outputs se acumulan en el mismo volumen.
- Si querés cambiar el browser o los productos, editá `HIT7/k8s/configmap.yaml` y hacé `kubectl apply -f HIT7/k8s/configmap.yaml`.

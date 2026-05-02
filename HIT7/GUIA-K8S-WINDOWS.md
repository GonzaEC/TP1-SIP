# Guía paso a paso: Kubernetes en Windows con k3d

> Esta guía asume que no sabés nada de Kubernetes y estás en Windows.
> Usamos **k3d** porque es la forma más fácil: corre k3s (Kubernetes liviano) dentro de contenedores Docker.

---

## Pre-requisitos

- **Docker Desktop** instalado y corriendo.
- **PowerShell** (se recomienda ejecutar como Administrador).

---

## Paso 1: Instalar k3d (el cluster Kubernetes)

Abrí PowerShell como Administrador y ejecutá:

```powershell
# Opción A: Con Chocolatey (recomendado)
choco install k3d

# Opción B: Sin Chocolatey — descargar el .exe desde:
# https://github.com/k3d-io/k3d/releases
# y agregarlo a tu PATH.
```

Si no tenés Chocolatey, instalalo primero:

```powershell
Set-ExecutionPolicy Bypass -Scope Process -Force; [System.Net.ServicePointManager]::SecurityProtocol = [System.Net.ServicePointManager]::SecurityProtocol -bor 3072; iex ((New-Object System.Net.WebClient).DownloadString('https://community.chocolatey.org/install.ps1'))
```

---

## Paso 2: Instalar kubectl (el cliente de Kubernetes)

En la misma PowerShell:

```powershell
choco install kubernetes-cli
```

Verificá que esté instalado:

```powershell
kubectl version --client
```

Debería mostrarte la versión del cliente.

---

## Paso 3: Crear el cluster k3d

```powershell
k3d cluster create scraper
```

Esto tarda entre 30 y 60 segundos. Verificá que esté funcionando:

```powershell
kubectl get nodes
```

Deberías ver algo como:

```
NAME                       STATUS   ROLES                  AGE   VERSION
k3d-scraper-server-0       Ready    control-plane,master   10s   v1.30.x
```

---

## Paso 4: Construir la imagen Docker del scraper

Andá a la carpeta `HIT6` del proyecto y construí la imagen:

```powershell
cd E:\Gonza\Programacion\ejercicio-SIP\TP1-SIP\HIT6
docker build -t ml-scraper:latest .
```

---

## Paso 5: Cargar la imagen en el cluster k3d

Como el cluster corre dentro de Docker, no ve las imágenes de tu máquina automáticamente. Tenés que importarla:

```powershell
k3d image import ml-scraper:latest -c scraper
```

---

## Paso 6: Aplicar los manifiestos de Kubernetes

Andá a la raíz del proyecto y aplicá los manifiestos:

```powershell
cd E:\Gonza\Programacion\ejercicio-SIP\TP1-SIP
kubectl apply -f HIT7/k8s/
```

Esto crea:
- ConfigMap (configuración del scraper)
- PVC (volumen persistente para los outputs)
- Job (ejecución one-off)
- CronJob (ejecución programada cada hora)

Verificá que se crearon correctamente:

```powershell
kubectl get configmap
kubectl get pvc
kubectl get jobs
kubectl get cronjobs
```

---

## Paso 7: Ver los logs del scraper

```powershell
# Ver el pod que ejecutó el Job
kubectl get pods -l job-name=scraper-once

# Ver los logs del scraper
kubectl logs -l job-name=scraper-once
```

---

## Paso 8: Verificar que generó los JSONs y screenshots

```powershell
# Encontrar el nombre exacto del pod
$POD = kubectl get pod -l job-name=scraper-once -o jsonpath="{.items[0].metadata.name}"

# Listar los archivos generados
kubectl exec $POD -- ls -la /app/output
kubectl exec $POD -- ls -la /app/screenshots
```

---

## Paso 9: Verificar el CronJob

```powershell
# Ver el estado del CronJob
kubectl get cronjob scraper-hourly

# Ver los jobs creados por el cron (aparecen cada hora)
kubectl get jobs --watch
```

---

## Limpieza (borrar todo)

### Opción A: Borrar solo los recursos de Kubernetes

```powershell
kubectl delete -f HIT7/k8s/
```

### Opción B: Borrar TODO el cluster (más drástico)

```powershell
k3d cluster delete scraper
```

---

## Comandos útiles de referencia

| Comando | Qué hace |
|---|---|
| `kubectl get nodes` | Ver nodos del cluster |
| `kubectl get pods` | Ver pods corriendo |
| `kubectl get jobs` | Ver jobs ejecutados |
| `kubectl get cronjobs` | Ver cronjobs programados |
| `kubectl logs <pod>` | Ver logs de un pod |
| `kubectl describe pod <pod>` | Ver detalle de un pod (útil para debug) |
| `k3d cluster list` | Ver clusters de k3d |
| `k3d cluster delete scraper` | Borrar el cluster |

---

## Solución de problemas

### "Docker is not running"
Asegurate de que Docker Desktop esté abierto y el engine esté corriendo.

### "kubectl no se reconoce como un comando"
Cerrá y volvé a abrir PowerShell después de instalar kubectl.

### "ImagePullBackOff" o "ErrImagePull"
La imagen no está cargada en el cluster. Rehacé el paso 5 (`k3d image import`).

### No aparecen los JSONs en `/app/output`
El scraper puede haber fallado por bloqueo de Mercado Libre. Verificá los logs con `kubectl logs -l job-name=scraper-once`.

---

## ¿Y ahora?

Si llegaste hasta acá y los logs muestran `[SUCCESS] JSON: ...`, ¡felicitaciones! El scraper está corriendo en Kubernetes.

Para la entrega, la cátedra suele pedir:
1. Una captura de `kubectl get jobs` mostrando un Job completado.
2. Una captura de `kubectl get cronjobs` mostrando el cron activo.
3. El contenido del README de HIT7 (ya está escrito en `HIT7/README.md`).

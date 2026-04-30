# Hit #4: Extracción Multi-producto y JSON

Este hit generaliza el scraper para procesar una lista de productos y extraer información detallada en formato JSON.

## Requerimientos cumplidos
- Procesa una lista de productos: `bicicleta rodado 29`, `iPhone 16 Pro Max`, `GeForce RTX 5090`.
- Extrae los primeros 10 resultados filtrados por cada producto.
- Campos capturados:
    - `titulo`: Título del producto.
    - `precio`: Valor numérico en ARS.
    - `link`: URL completa.
    - `tienda_oficial`: Nombre de la tienda (si aplica).
    - `envio_gratis`: Booleano.
    - `cuotas_sin_interes`: Descripción de cuotas (si aplica).
- Genera archivos JSON en la carpeta `output/`.
- Mantiene la funcionalidad de screenshots de Hit #3.

## Ejecución
Desde la carpeta `HIT4`:
```powershell
mvn clean compile exec:java
```

O especificando el navegador:
```powershell
mvn clean compile exec:java -Dbrowser=firefox
```

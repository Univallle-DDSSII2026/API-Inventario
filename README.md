# API de Inventario — ejemplo transaccional (Equipo 5)

Ejemplo de punta a punta del proceso crítico del DAS (Figura 6): la **salida por pedido de productos terminados** (HU_CUTIT_04) con la **notificación de stock bajo** (HU_CUTIT_17). Recorre las capas del DAS:

```
HTTP → RateLimitFilter → SecurityFilter (View, API Key) → MovimientoController (View) → SalidaPedidoRequest (DTO) → MovimientoService (Logic, @Transactional)
     → ValidadorSalida (Logic) → ArticuloRepository / MovimientoRepository (Data Access) → PostgreSQL
     → respuesta 201/422 → después del commit: NotificacionesClient (Integration) → API de Notificaciones
```

Qué demuestra:

- **Una transacción por solicitud**: se registran las líneas válidas y se rechazan solo las inválidas; si todas fallan, no se guarda nada (422).
- **Bloqueo de filas** (`SELECT … FOR UPDATE`): diez salidas simultáneas nunca dejan existencias negativas.
- **Seguridad con API Key por sistema cliente**: el `SecurityFilter` valida el encabezado `X-API-Key` y asigna el rol (FABRICACION, NOTIFICACIONES o EQUIPO); sin clave o con una clave inválida responde 401, y si el rol no tiene permiso, 403 (`@PreAuthorize`).
- **Rate limit**: el `RateLimitFilter` (Bucket4j) responde 429 si un cliente pasa de `RATE_LIMIT_PER_MINUTE` peticiones por minuto.
- **Outbox**: la notificación se guarda en la misma transacción y se envía después del commit; si la API de Notificaciones falla, la salida no se revierte.

## Requisitos

Docker Desktop 4.x (Docker Compose 2.20 o superior) y, para correr las pruebas sin Docker Compose, JDK 21.

## Ejecutar

```bash
cp .env.example .env
docker compose up --build
```

Abra <http://localhost:8080/swagger-ui.html>, haga clic en **Authorize**, escriba la clave del Equipo 4 que puso en `API_KEYS` (en el `.env.example` es `clave-equipo-cambiar`) y ejecute `POST /inventario/salidas/pedidos/productos-terminados` con la fecha de hoy:

```json
{
  "referenciaPedido": "PED-2026-0042",
  "fecha": "07/11/2026",
  "lineas": [
    { "codigoArticulo": "PT-00001", "cantidad": 6 },
    { "codigoArticulo": "PT-00099", "cantidad": 1 }
  ]
}
```

Respuesta 201: la línea 1 se registra y la 2 se rechaza («no existe un artículo con el código PT-00099»). PT-00001 (Cadena Ripit 5000) baja de 10 a 4, por debajo de su umbral de 5, y se envía la notificación. Para verlo en la base de datos:

```bash
docker compose exec db psql -U inventario_app -d inventario -c "SELECT codigo, existencias FROM articulo;"
docker compose exec db psql -U inventario_app -d inventario -c "SELECT id, estado, fecha_envio FROM notificacion;"
```

Artículos de demostración: PT-00001 (10 existencias, umbral 5), PT-00002 (3, umbral 1) y MP-00001 (materia prima, sirve para ver el rechazo «no es un producto terminado»).

Sin la clave, la respuesta es 401; con la clave de Fabricación, 403. La documentación (`/swagger-ui.html`, `/v3/api-docs`) y `/actuator/health` no piden clave.

## Pruebas y cobertura

```bash
./gradlew test                      # unitarias (validador y servicio)
./gradlew integrationTest           # integración con PostgreSQL real (requiere Docker)
./gradlew jacocoTestReport jacocoTestCoverageVerification   # reporte y regla >= 80 %
```

Reporte: `build/reports/jacoco/jacocoTestReport/html/index.html`. En Windows use `gradlew.bat`.

## Qué queda fuera de este ejemplo

El monitoreo con Prometheus y Grafana (se agrega después, según indique el profesor), reintentos programados de notificaciones, validación de los 30 días de antigüedad de la fecha y las demás historias de usuario. Están en el DAS y se agregan sobre esta misma estructura.

# Parcial ArquiSW: plantilla REST + HATEOAS + versionamiento + Docker + observabilidad

Plantilla lista para el parcial práctico de Arquitectura de Software (UdeA). Trae un dominio de ejemplo (**Producto** y **Orden**) que funciona de punta a punta; el día del parcial se reemplaza por el del enunciado.

> Plan paso a paso para las 3 horas: [GUIA-PARCIAL.md](GUIA-PARCIAL.md) · Plantilla del documento de entrega: [docs/INFORME.md](docs/INFORME.md)

## Qué cumple

| Requisito del parcial | Cómo se cumple |
|---|---|
| Servicios web RESTful | Spring Boot 3.5 (Java 21), verbos y códigos HTTP correctos (200, 201 + `Location`, 204, 400, 404, 409) |
| **HATEOAS** | Spring HATEOAS con HAL (`_links`, `_embedded`). Punto de entrada `/api` para descubrir la API. Los links de acciones cambian según el estado (una orden `CREADA` ofrece `pagar`; una `ENVIADA`, ninguna acción) |
| **Versionamiento** | Por URI: `/api/v1/...` y `/api/v2/...`. v2 cambia la representación (precio con moneda, inventario) y agrega paginación, filtro y orden. Encabezado `X-API-Version` en cada respuesta y links `version-1` / `version-2` entre versiones |
| Base de datos | PostgreSQL 17 (JPA, bloqueo optimista con `@Version`) |
| **Docker** | `Dockerfile` multi-stage y `docker-compose.yml` con 5 servicios |
| **Observabilidad** | **Prometheus** (métricas de Actuator/Micrometer + reglas de alerta), **Grafana** (dashboard ya cargado), **Loki** (logs en JSON con `requestId`) |
| Pruebas | 7 pruebas de integración (JUnit + MockMvc) y colección Postman de 17 pasos con Newman |
| Documentación | Swagger UI con un grupo por versión, errores RFC 9457 (`application/problem+json`) |

## Arquitectura

```
                 ┌──────────────────────── docker compose ────────────────────────┐
 Cliente ──HTTP──► api :8080 ──JDBC──► db (PostgreSQL) :5432                      │
 (Postman,       │   │  │                                                         │
  navegador)     │   │  └──logs JSON (push)──► loki :3100 ◄──┐                    │
                 │   └──/actuator/prometheus◄── prometheus :9090 ◄── grafana :3000│
                 └────────────────────────────────────────────────────────────────┘
```

## Arrancar

Requisitos: Docker Desktop. Para las pruebas locales: JDK 21 y Node.js.

```powershell
docker compose up -d --build
docker compose ps          # esperar a que api esté "healthy" (~40 s)
```

| Servicio | URL | Usuario |
|---|---|---|
| API (entrada HATEOAS) | http://localhost:8080/api | |
| Swagger UI | http://localhost:8080/swagger-ui.html | |
| Salud / métricas | http://localhost:8080/actuator/health · `/actuator/prometheus` | |
| Grafana | http://localhost:3000 → Dashboards → Parcial → **Parcial API** | admin / admin |
| Prometheus | http://localhost:9090 (Status → Targets, Alerts) | |
| Logs | Grafana → Explore → Loki → `{app="parcial-api"}` | |

Para apagar: `docker compose down` (agregar `-v` para borrar también los datos).

## Probar

```powershell
# Pruebas automáticas (no necesitan Docker)
cd api; .\mvnw.cmd test; cd ..

# Colección Postman con Newman contra la API en Docker (guarda docs/evidencias/newman-resultado.json)
.\scripts\probar-api.ps1

# Tráfico durante 2 minutos para llenar el dashboard de Grafana
.\scripts\generar-trafico.ps1 -Segundos 120
```

La colección también se puede importar en Postman: `pruebas/parcial.postman_collection.json`.

## Endpoints

| Método | v1 | v2 | Qué hace |
|---|---|---|---|
| GET | `/api` | | Raíz: links a todos los recursos y versiones |
| GET | `/api/v1/productos` | `/api/v2/productos?page=&size=&sort=&categoria=` | Listar (v2 paginado) |
| GET | `/api/v1/productos/{id}` | `/api/v2/productos/{id}` | Obtener |
| POST | `/api/v1/productos` | `/api/v2/productos` | Crear (201 + `Location`) |
| PUT | `/api/v1/productos/{id}` | `/api/v2/productos/{id}` | Actualizar |
| DELETE | `/api/v1/productos/{id}` | `/api/v2/productos/{id}` | Eliminar (409 si tiene órdenes) |
| GET | `/api/v1/ordenes?estado=` | | Listar órdenes |
| GET | `/api/v1/ordenes/{id}` | | Obtener orden con sus acciones |
| POST | `/api/v1/ordenes` | | Crear orden (descuenta stock) |
| POST | `/api/v1/ordenes/{id}/pagar` · `/enviar` · `/cancelar` | | Transiciones de estado |

Ejemplo de respuesta de una orden recién creada:

```json
{
  "id": 1, "cliente": "Ana", "productoId": 2, "producto": "Mouse inalámbrico",
  "cantidad": 2, "total": 138000.00, "estado": "CREADA",
  "_links": {
    "self":     { "href": "http://localhost:8080/api/v1/ordenes/1" },
    "ordenes":  { "href": "http://localhost:8080/api/v1/ordenes{?estado}", "templated": true },
    "producto": { "href": "http://localhost:8080/api/v1/productos/2" },
    "pagar":    { "href": "http://localhost:8080/api/v1/ordenes/1/pagar" },
    "cancelar": { "href": "http://localhost:8080/api/v1/ordenes/1/cancelar" }
  }
}
```

## Estructura

```
api/                         Spring Boot (código, Dockerfile, pruebas)
  src/main/java/com/udea/parcial/
    dominio/                 Entidades y reglas de negocio  ← cambiar por el enunciado
    repositorio/             Spring Data JPA
    servicio/                Lógica + métricas + logs
    web/v1, web/v2           Controladores y assemblers HATEOAS por versión
    web/error                Manejo de errores (Problem Detail)
    config/                  Filtro de logs/requestId, datos iniciales, OpenAPI
observabilidad/              Prometheus (+ alertas), Grafana (datasources + dashboard)
pruebas/                     Colección Postman (generada por generar-coleccion.js)
scripts/                     probar-api.ps1, generar-trafico.ps1
docs/                        INFORME.md (plantilla de entrega) y evidencias/
```

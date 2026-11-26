# Parcial práctico final: [nombre de la solución]

**Universidad de Antioquia · Arquitectura de Software**
**Profesor:** Diego José Luis Botia Valderrama
**Integrantes:** [nombre 1], [nombre 2], [nombre 3]
**Fecha:** [fecha]
**Repositorio:** [URL del repositorio en GitHub]

> Plantilla: reemplazar todo lo que está entre corchetes y las capturas de `evidencias/`.

## 1. Enunciado

[Copiar el problema asignado al grupo.]

## 2. Solución propuesta

[Dos o tres párrafos: qué recursos se modelaron, qué reglas de negocio hay y qué hace cada versión de la API.]

### 2.1 Tecnologías

| Capa | Tecnología | Por qué |
|---|---|---|
| API | Java 21, Spring Boot 3.5, Spring Web | Estándar del curso, arranque rápido |
| HATEOAS | Spring HATEOAS (formato HAL) | Links generados a partir de los controladores, sin URLs a mano |
| Datos | PostgreSQL 17 con Spring Data JPA | Base relacional; bloqueo optimista con `@Version` |
| Contenedores | Docker, Docker Compose | Todo el sistema arranca con un comando |
| Métricas | Spring Boot Actuator, Micrometer, Prometheus | Métricas HTTP, JVM y de negocio |
| Logs | Logback + Loki | Logs en JSON con `requestId`, consultables desde Grafana |
| Visualización | Grafana | Dashboard y fuentes de datos configurados como código |
| Documentación | springdoc-openapi (Swagger UI) | Un grupo por versión |
| Pruebas | JUnit 5 + MockMvc, Postman + Newman | Pruebas de integración y de la API desplegada |

### 2.2 Arquitectura

```
                 ┌──────────────────────── docker compose ────────────────────────┐
 Cliente ──HTTP──► api :8080 ──JDBC──► db (PostgreSQL) :5432                      │
                 │   │  └──logs JSON──► loki :3100 ◄──┐                           │
                 │   └──/actuator/prometheus◄── prometheus :9090 ◄── grafana :3000│
                 └────────────────────────────────────────────────────────────────┘
```

La API sigue una arquitectura por capas: `web` (controladores y assemblers HATEOAS, una carpeta por versión) → `servicio` (casos de uso, métricas y logs) → `dominio` (entidades y reglas) → `repositorio` (Spring Data JPA). Las dos versiones comparten el servicio y el dominio; solo cambia la representación.

### 2.3 Modelo de datos

[Diagrama o tabla de entidades, campos y relaciones.]

### 2.4 Estados y reglas de negocio

[Diagrama de estados, por ejemplo `CREADA → PAGADA → ENVIADA`, `CREADA/PAGADA → CANCELADA`, y las reglas: stock, transiciones válidas, etc.]

## 3. Diseño de la API

### 3.1 Recursos y endpoints

[Tabla de endpoints v1 y v2: método, ruta, códigos de respuesta.]

### 3.2 HATEOAS

Las respuestas usan HAL (`application/hal+json`): cada recurso trae `_links` y las colecciones traen `_embedded`. El cliente entra por `GET /api` y descubre el resto siguiendo links.

Los links de acciones dependen del estado del recurso. Por ejemplo, una orden `CREADA` trae `pagar` y `cancelar`; al pagarla, trae `enviar` y `cancelar`; y una orden `ENVIADA` ya no trae acciones. Así el cliente no necesita conocer las reglas de transición: solo sigue los links que recibe.

![Raíz HATEOAS](evidencias/03-raiz-hateoas.png)
![Orden creada](evidencias/06-orden-creada.png)
![Orden pagada](evidencias/07-orden-pagada.png)

### 3.3 Versionamiento

Estrategia: **versión en la URI** (`/api/v1`, `/api/v2`). Es explícita, fácil de probar y de cachear. Cada respuesta incluye el encabezado `X-API-Version`, y cada recurso tiene links al mismo recurso en la otra versión (`version-1`, `version-2`).

| Aspecto | v1 | v2 |
|---|---|---|
| Listado | Completo, sin paginar | Paginado (`page`, `size`, `sort`) con links `first`/`prev`/`next`/`last` |
| Filtros | No | `categoria` |
| Representación | Plana: `precio`, `stock` | `precio: {valor, moneda}`, `inventario: {stock, disponible}`, descripción, categoría, fecha |

v1 se mantiene para no romper a los clientes existentes; los clientes nuevos usan v2.

![v1](evidencias/04-v1-producto.png)
![v2 paginado](evidencias/05-v2-paginado.png)

### 3.4 Manejo de errores

Los errores usan el formato estándar Problem Detail (RFC 9457, `application/problem+json`):

| Código | Cuándo |
|---|---|
| 400 | Datos inválidos (con el detalle por campo en `errores`) o parámetros mal formados |
| 404 | El recurso no existe |
| 409 | Se viola una regla de negocio (transición de estado inválida, stock insuficiente, recurso con dependencias) o hay un conflicto de concurrencia |

![Error 409](evidencias/08-error-409.png)

## 4. Despliegue con Docker

`docker-compose.yml` define 5 servicios: `api`, `db`, `prometheus`, `loki` y `grafana`. La imagen de la API se construye con un `Dockerfile` multi-stage (compila con Maven y ejecuta con un JRE Alpine, con un usuario sin privilegios y un `HEALTHCHECK` contra `/actuator/health`). La API espera a que PostgreSQL esté sano antes de arrancar.

```powershell
docker compose up -d --build
docker compose ps
```

![docker compose ps](evidencias/01-docker-ps.png)
![Docker Desktop](evidencias/02-docker-desktop.png)

## 5. Observabilidad

### 5.1 Métricas (Prometheus)

La API publica sus métricas en `/actuator/prometheus` y Prometheus las recoge cada 5 s. Además de las métricas HTTP (con histograma para calcular percentiles) y de la JVM, hay una métrica de negocio, `parcial_ordenes_total{evento}`, que cuenta las órdenes creadas, pagadas, enviadas y canceladas.

Reglas de alerta configuradas: API caída, tasa de errores 5xx alta y latencia p95 mayor a 500 ms.

![Targets](evidencias/12-prometheus-targets.png)
![Consulta](evidencias/13-prometheus-consulta.png)
![Alertas](evidencias/16-prometheus-alertas.png)

### 5.2 Logs (Loki)

Cada petición recibe un `X-Request-Id` (o se reutiliza el que mande el cliente), que se guarda en el contexto de los logs. Con el perfil `docker`, los logs se envían a Loki en formato JSON con las etiquetas `app` y `level`, así que se pueden filtrar por nivel o buscar todas las líneas de una misma petición.

![Logs](evidencias/15-grafana-logs.png)

### 5.3 Dashboard (Grafana)

El dashboard **Parcial API** se carga automáticamente al arrancar Grafana. Muestra: estado de la API, peticiones por segundo (total, por endpoint y por versión), latencia p50/p95/p99, respuestas por código HTTP, porcentaje de errores, órdenes por evento, memoria de la JVM y los logs.

![Dashboard](evidencias/14-grafana-dashboard.png)

## 6. Pruebas

### 6.1 Pruebas de integración

[N] pruebas con JUnit y MockMvc sobre una base H2 en memoria: descubrimiento desde la raíz, colección HAL de v1, paginación y representación de v2, CRUD con `Location`, links de la orden según su estado, errores 400/404/409 y exposición de métricas.

![mvn test](evidencias/11-mvn-test.png)

### 6.2 Pruebas de la API desplegada (Postman + Newman)

La colección `pruebas/parcial.postman_collection.json` recorre [N] pasos contra la API en Docker siguiendo los links HATEOAS (después del primer paso no se escribe ninguna URL a mano).

| # | Prueba | Esperado | Obtenido |
|---|---|---|---|
| 1 | `GET /api` | Links a v1 y v2 | [ ] |
| 2 | Listar v1 | Colección HAL, `X-API-Version: 1` | [ ] |
| 3 | Crear producto | 201 + `Location` + links | [ ] |
| 4 | Obtener por link `self` | 200 | [ ] |
| 5 | Actualizar | 200, precio nuevo | [ ] |
| 6 | Mismo producto en v2 | Precio con moneda, `X-API-Version: 2` | [ ] |
| 7 | v2 paginado y ordenado | Página de 3, links `next`/`last` | [ ] |
| 8 | v2 filtrado | Solo la categoría pedida | [ ] |
| 9 | Crear orden | 201, `CREADA`, links `pagar`/`cancelar` | [ ] |
| 10 | Pagar | `PAGADA`, link `enviar` | [ ] |
| 11 | Enviar | `ENVIADA`, sin acciones | [ ] |
| 12 | Cancelar orden enviada | 409 Problem Detail | [ ] |
| 13 | Producto inválido | 400 con errores por campo | [ ] |
| 14 | Producto inexistente | 404 | [ ] |
| 15 | Eliminar producto con órdenes | 409 | [ ] |
| 16 | Salud | `UP` | [ ] |
| 17 | Métricas | Métricas HTTP y de negocio | [ ] |

![Newman](evidencias/10-newman.png)

## 7. Documentación de la API

Swagger UI en `http://localhost:8080/swagger-ui.html`, con un grupo para cada versión.

![Swagger](evidencias/09-swagger.png)

## 8. Problemas encontrados

| Problema | Causa | Solución |
|---|---|---|
| [ ] | [ ] | [ ] |

## 9. Conclusiones

- [Sobre HATEOAS: qué aporta al cliente.]
- [Sobre el versionamiento: cómo permite evolucionar sin romper clientes.]
- [Sobre Docker: reproducibilidad.]
- [Sobre la observabilidad: qué permitió ver o detectar.]

# Guía para el día del parcial

Viernes · Zoom a las **9:55 a. m.** · entrega máxima **1:00 p. m.** (3 horas).

## Antes de empezar (la noche anterior)

- [ ] Docker Desktop abre y `docker compose up -d --build` levanta los 5 servicios con esta plantilla.
- [ ] `cd api; .\mvnw.cmd test` pasa (deja descargadas las dependencias de Maven).
- [ ] `.\scripts\probar-api.ps1` pasa (deja Newman en la caché de npm).
- [ ] Grafana muestra datos en el dashboard **Parcial API** después de `.\scripts\generar-trafico.ps1`.
- [ ] La plantilla del informe está abierta y sé cómo exportar Markdown a PDF (por ejemplo, la extensión *Markdown PDF* de VS Code).

## Trabajando solo: prioridades

En 3 horas y sin equipo, lo importante es **entregar algo completo antes que algo perfecto**. La plantilla ya cumple todos los requisitos, así que el orden es:

1. **Primero, que funcione con el dominio del enunciado**: renombrar entidades y campos, y que `docker compose up` siga arrancando. Con eso ya se cumple REST, HATEOAS, versionamiento, Docker y observabilidad.
2. **Después, las evidencias**: capturas y el informe. Sin documento la entrega queda incompleta.
3. **Solo si sobra tiempo**: reglas de negocio adicionales, más pruebas o más paneles en Grafana.

Si a las 11:45 el dominio nuevo todavía no compila, entregar con lo que sí funciona y explicarlo en el informe.

## Plan de tiempo

| Hora | Qué hacer |
|---|---|
| 9:55 – 10:10 | Leer el enunciado. Anotar recursos, campos, estados y reglas. Decidir qué cambia en v2. Crear el repositorio de entrega |
| 10:10 – 11:10 | Adaptar el dominio y los controladores (ver lista abajo). `mvnw test` cada vez que algo compile. **Push** |
| 11:10 – 11:40 | `docker compose up -d --build`, ajustar la colección Postman y correr `probar-api.ps1`. **Push** |
| 11:40 – 12:30 | Generar tráfico, tomar las capturas, llenar `docs/INFORME.md` |
| 12:30 – 12:45 | Exportar el informe a PDF, subirlo al repositorio, último **push** y revisar en GitHub |
| 12:45 – 13:00 | Margen. **No dejar el push para el último minuto** |

## Crear el repositorio de entrega

Este repositorio está marcado como **plantilla** en GitHub, así que el de entrega se crea con una copia limpia, sin el historial:

```powershell
cd C:\Users\frito\Documents\GitHub
gh repo create parcial-arquisw-final --public --template DavidAraangoPineda/parcial-arquisw-plantilla --clone
cd parcial-arquisw-final
```

También se puede hacer desde la web: botón **Use this template** → **Create a new repository**.

Antes de levantar el repositorio nuevo, apagar el de la plantilla para liberar los puertos: `docker compose down` en la carpeta de la plantilla.

## Adaptar al enunciado: qué archivos tocar

Ejemplo: si el enunciado es una biblioteca, `Producto` pasa a ser `Libro` y `Orden` pasa a ser `Prestamo` (estados `ACTIVO → DEVUELTO`, `VENCIDO`).

1. **`dominio/`**: renombrar `Producto` y `Orden` (en IntelliJ: Shift+F6 renombra también todas las referencias), cambiar campos y reglas. `EstadoOrden` define las transiciones; los links HATEOAS salen de sus métodos `puede...()`.
2. **`web/ProductoRequest`, `web/OrdenRequest`**: campos y validaciones (`@NotBlank`, `@Min`, etc.).
3. **`web/v1/*V1.java`, `web/v2/*V2.java`**: qué campos muestra cada versión. Mantener una diferencia clara entre v1 y v2.
4. **`web/v1/*AssemblerV1.java`**: los links. Para cada transición de estado, un `addIf(estado.puede...(), ...)`.
5. **`config/DatosIniciales.java`**: datos de ejemplo del enunciado.
6. **`servicio/OrdenService.registrar`**: el nombre de la métrica de negocio (`parcial.ordenes`). Si cambia, actualizar el panel "métrica de negocio" del dashboard de Grafana.
7. **`ApiIntegracionTest`** y **`pruebas/generar-coleccion.js`**: cambiar rutas, campos y valores esperados.

Si la base de datos cambia de forma incompatible: `docker compose down -v` borra los datos y Hibernate crea las tablas de nuevo.

### Si piden NoSQL

Cambiar `spring-boot-starter-data-jpa` + `postgresql` por `spring-boot-starter-data-mongodb`, `@Entity` por `@Document`, `JpaRepository` por `MongoRepository`, `Long id` por `String id`, y en `docker-compose.yml` el servicio `db` por `mongo:8` con `SPRING_DATA_MONGODB_URI=mongodb://db:27017/parcial`. Las capas web, HATEOAS y observabilidad no cambian.

### Si piden versionamiento por encabezado o media type

El versionamiento por URI es el más fácil de evidenciar. Si piden otra estrategia, se puede agregar sobre los mismos controladores:

```java
// Encabezado:  GET /api/productos   con  X-API-Version: 2
@GetMapping(value = "/api/productos", headers = "X-API-Version=2")

// Media type:  Accept: application/vnd.parcial.v2+hal+json
@GetMapping(value = "/api/productos", produces = "application/vnd.parcial.v2+hal+json")
```

## Evidencias que hay que capturar

Guardar en `docs/evidencias/` con estos nombres para que el informe ya las enlace:

| Archivo | Qué mostrar |
|---|---|
| `01-docker-ps.png` | `docker compose ps` con los 5 servicios arriba y la API `healthy` |
| `02-docker-desktop.png` | Docker Desktop con el proyecto `parcial` |
| `03-raiz-hateoas.png` | `GET /api` con los `_links` (Postman o navegador) |
| `04-v1-producto.png` | `GET /api/v1/productos/{id}` con sus links |
| `05-v2-paginado.png` | `GET /api/v2/productos?size=3` con `page` y links `next`/`last` |
| `06-orden-creada.png` | Orden `CREADA` con links `pagar` y `cancelar` |
| `07-orden-pagada.png` | La misma orden `PAGADA` con link `enviar` |
| `08-error-409.png` | Respuesta Problem Detail de una transición inválida |
| `09-swagger.png` | Swagger UI con los grupos v1 y v2 |
| `10-newman.png` | Resultado de `probar-api.ps1` (todas las pruebas en verde) |
| `11-mvn-test.png` | `mvnw test` con `Tests run: N, Failures: 0` |
| `12-prometheus-targets.png` | Prometheus → Status → Targets con `parcial-api` en `UP` |
| `13-prometheus-consulta.png` | Gráfica de `rate(http_server_requests_seconds_count[1m])` |
| `14-grafana-dashboard.png` | Dashboard **Parcial API** con datos |
| `15-grafana-logs.png` | Panel de logs o Explore con `{app="parcial-api"}` y un `requestId` |
| `16-prometheus-alertas.png` | Prometheus → Alerts con las 3 reglas |

## Si algo falla

| Síntoma | Qué hacer |
|---|---|
| La API se reinicia en bucle | `docker compose logs api`. Casi siempre es la base de datos: revisar `docker compose ps db` |
| Puerto ocupado (8080, 3000, 9090…) | Cambiar el puerto de la izquierda en `docker-compose.yml`, por ejemplo `"8081:8080"` |
| Grafana sin datos | Revisar que Prometheus vea el target en `UP` y generar tráfico. El rango de tiempo del dashboard debe ser "últimos 15 min" |
| No aparecen logs en Loki | Solo se envían con el perfil `docker`; revisar `SPRING_PROFILES_ACTIVE` y `docker compose logs loki` |
| Cambié código y no se refleja | `docker compose up -d --build api` |
| Error de esquema tras cambiar entidades | `docker compose down -v` y volver a levantar |

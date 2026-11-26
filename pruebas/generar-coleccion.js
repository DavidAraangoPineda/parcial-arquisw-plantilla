// Genera pruebas/parcial.postman_collection.json.
// Se escribe en JS para que sea fácil agregar o cambiar pasos el día del parcial:
//   node pruebas/generar-coleccion.js
// La colección sigue los links HATEOAS (no arma URLs a mano después del primer paso).
const fs = require("fs");
const path = require("path");

const paso = (nombre, method, url, tests, body) => ({
  name: nombre,
  event: [{ listen: "test", script: { type: "text/javascript", exec: tests.trim().split("\n") } }],
  request: {
    method,
    header: [
      // Actuator no habla HAL: para /actuator se acepta cualquier formato.
      { key: "Accept", value: url.includes("/actuator") ? "*/*" : "application/hal+json, application/problem+json" },
      ...(body ? [{ key: "Content-Type", value: "application/json" }] : []),
    ],
    url: url,
    ...(body ? { body: { mode: "raw", raw: JSON.stringify(body, null, 2) } } : {}),
  },
});

const items = [
  paso("01 Raíz: descubrir la API", "GET", "{{baseUrl}}/api", `
pm.test("200 OK", () => pm.response.to.have.status(200));
const links = pm.response.json()._links;
pm.test("Links a v1 y v2", () => {
  pm.expect(links).to.have.property("v1-productos");
  pm.expect(links).to.have.property("v1-ordenes");
  pm.expect(links).to.have.property("v2-productos");
});
pm.collectionVariables.set("v1Productos", links["v1-productos"].href);
pm.collectionVariables.set("v1Ordenes", links["v1-ordenes"].href.replace("{?estado}", ""));
pm.collectionVariables.set("v2Productos", links["v2-productos"].href.split("{")[0]);
`),
  paso("02 v1: listar productos", "GET", "{{v1Productos}}", `
pm.test("200 OK", () => pm.response.to.have.status(200));
pm.test("Encabezado X-API-Version = 1", () => pm.response.to.have.header("X-API-Version", "1"));
const json = pm.response.json();
pm.test("Colección HAL con _embedded y link crear", () => {
  pm.expect(json._embedded.productos.length).to.be.above(0);
  pm.expect(json._links).to.have.property("crear");
});
pm.test("Representación v1 sin campos de v2", () => pm.expect(json._embedded.productos[0]).to.not.have.property("categoria"));
`),
  paso("03 v1: crear producto", "POST", "{{v1Productos}}", `
pm.test("201 Created", () => pm.response.to.have.status(201));
pm.test("Encabezado Location", () => pm.response.to.have.header("Location"));
const json = pm.response.json();
pm.test("Links self, actualizar, eliminar, version-2 y ordenar", () => {
  ["self", "actualizar", "eliminar", "version-2", "ordenar"].forEach(r => pm.expect(json._links).to.have.property(r));
});
pm.collectionVariables.set("productoId", json.id);
pm.collectionVariables.set("productoSelf", json._links.self.href);
pm.collectionVariables.set("productoV2", json._links["version-2"].href);
pm.collectionVariables.set("ordenarUrl", json._links.ordenar.href);
`, { nombre: "Producto Postman", descripcion: "Creado por la prueba", categoria: "Pruebas", precio: 15000, stock: 10 }),
  paso("04 v1: obtener producto (link self)", "GET", "{{productoSelf}}", `
pm.test("200 OK", () => pm.response.to.have.status(200));
pm.test("Mismo producto", () => pm.expect(pm.response.json().nombre).to.eql("Producto Postman"));
`),
  paso("05 v1: actualizar producto", "PUT", "{{productoSelf}}", `
pm.test("200 OK", () => pm.response.to.have.status(200));
pm.test("Precio actualizado", () => pm.expect(pm.response.json().precio).to.eql(18000));
`, { nombre: "Producto Postman", descripcion: "Actualizado", categoria: "Pruebas", precio: 18000, stock: 10 }),
  paso("06 v2: mismo producto (link version-2)", "GET", "{{productoV2}}", `
pm.test("200 OK", () => pm.response.to.have.status(200));
pm.test("Encabezado X-API-Version = 2", () => pm.response.to.have.header("X-API-Version", "2"));
const json = pm.response.json();
pm.test("Representación v2: precio con moneda e inventario", () => {
  pm.expect(json.precio.moneda).to.eql("COP");
  pm.expect(json.inventario.disponible).to.eql(true);
  pm.expect(json.categoria).to.eql("Pruebas");
});
pm.test("Link de vuelta a v1", () => pm.expect(json._links).to.have.property("version-1"));
`),
  paso("07 v2: listar paginado", "GET", "{{v2Productos}}?page=0&size=3&sort=precio,desc", `
pm.test("200 OK", () => pm.response.to.have.status(200));
const json = pm.response.json();
pm.test("Página de 3 con metadatos", () => {
  pm.expect(json._embedded.productos.length).to.eql(3);
  pm.expect(json.page.size).to.eql(3);
});
pm.test("Links de navegación first/next/last", () => ["first", "next", "last"].forEach(r => pm.expect(json._links).to.have.property(r)));
pm.test("Ordenado por precio descendente", () => {
  const p = json._embedded.productos.map(x => x.precio.valor);
  pm.expect(p[0]).to.be.at.least(p[1]);
});
`),
  paso("08 v2: filtrar por categoría", "GET", "{{v2Productos}}?categoria=Tecnología", `
pm.test("200 OK", () => pm.response.to.have.status(200));
pm.test("Todos son de Tecnología", () => pm.response.json()._embedded.productos.forEach(p => pm.expect(p.categoria).to.eql("Tecnología")));
`),
  paso("09 v1: crear orden (link ordenar)", "POST", "{{ordenarUrl}}", `
pm.test("201 Created", () => pm.response.to.have.status(201));
const json = pm.response.json();
pm.test("Estado CREADA y total = precio x cantidad", () => {
  pm.expect(json.estado).to.eql("CREADA");
  pm.expect(json.total).to.eql(36000);
});
pm.test("HATEOAS: ofrece pagar y cancelar, no enviar", () => {
  pm.expect(json._links).to.have.property("pagar");
  pm.expect(json._links).to.have.property("cancelar");
  pm.expect(json._links).to.not.have.property("enviar");
});
pm.collectionVariables.set("pagarUrl", json._links.pagar.href);
`, { cliente: "Cliente Postman", productoId: "{{productoId}}", cantidad: 2 }),
  paso("10 v1: pagar orden (link pagar)", "POST", "{{pagarUrl}}", `
pm.test("200 OK", () => pm.response.to.have.status(200));
const json = pm.response.json();
pm.test("Estado PAGADA", () => pm.expect(json.estado).to.eql("PAGADA"));
pm.test("HATEOAS: ahora ofrece enviar, ya no pagar", () => {
  pm.expect(json._links).to.have.property("enviar");
  pm.expect(json._links).to.not.have.property("pagar");
});
pm.collectionVariables.set("enviarUrl", json._links.enviar.href);
pm.collectionVariables.set("cancelarUrl", json._links.cancelar.href);
`),
  paso("11 v1: enviar orden (link enviar)", "POST", "{{enviarUrl}}", `
pm.test("200 OK", () => pm.response.to.have.status(200));
const json = pm.response.json();
pm.test("Estado ENVIADA y sin acciones", () => {
  pm.expect(json.estado).to.eql("ENVIADA");
  ["pagar", "enviar", "cancelar"].forEach(r => pm.expect(json._links).to.not.have.property(r));
});
`),
  paso("12 Error 409: cancelar orden enviada", "POST", "{{cancelarUrl}}", `
pm.test("409 Conflict", () => pm.response.to.have.status(409));
pm.test("Problem Detail", () => {
  pm.expect(pm.response.headers.get("Content-Type")).to.include("application/problem+json");
  pm.expect(pm.response.json().detail).to.include("ENVIADA");
});
`),
  paso("13 Error 400: producto inválido", "POST", "{{v1Productos}}", `
pm.test("400 Bad Request", () => pm.response.to.have.status(400));
pm.test("Detalle por campo", () => {
  const e = pm.response.json().errores;
  pm.expect(e).to.have.property("nombre");
  pm.expect(e).to.have.property("precio");
});
`, { nombre: "", categoria: "X", precio: -5, stock: 1 }),
  paso("14 Error 404: producto inexistente", "GET", "{{v2Productos}}/999999", `
pm.test("404 Not Found", () => pm.response.to.have.status(404));
pm.test("Mensaje claro", () => pm.expect(pm.response.json().detail).to.include("999999"));
`),
  paso("15 Error 409: eliminar producto con órdenes", "DELETE", "{{productoSelf}}", `
pm.test("409 Conflict", () => pm.response.to.have.status(409));
`),
  paso("16 Observabilidad: salud", "GET", "{{baseUrl}}/actuator/health", `
pm.test("UP", () => pm.expect(pm.response.json().status).to.eql("UP"));
`),
  paso("17 Observabilidad: métricas Prometheus", "GET", "{{baseUrl}}/actuator/prometheus", `
pm.test("200 OK", () => pm.response.to.have.status(200));
pm.test("Métricas HTTP y de negocio", () => {
  pm.expect(pm.response.text()).to.include("http_server_requests_seconds_count");
  pm.expect(pm.response.text()).to.include("parcial_ordenes_total");
});
`),
];

const coleccion = {
  info: {
    name: "Parcial ArquiSW - API HATEOAS v1/v2",
    schema: "https://schema.getpostman.com/json/collection/v2.1.0/collection.json",
  },
  variable: [{ key: "baseUrl", value: "http://localhost:8080" }],
  item: items,
};

// productoId va como número en el JSON de la orden.
const salida = JSON.stringify(coleccion, null, 2).replace('\\"productoId\\": \\"{{productoId}}\\"', '\\"productoId\\": {{productoId}}');
fs.writeFileSync(path.join(__dirname, "parcial.postman_collection.json"), salida);
console.log("Colección generada con " + items.length + " pasos.");

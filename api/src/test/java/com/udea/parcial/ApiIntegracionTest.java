package com.udea.parcial;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Pruebas de punta a punta de la API: HATEOAS, versiones, reglas de negocio, errores y métricas. */
@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureObservability
class ApiIntegracionTest {

    @Autowired
    MockMvc mvc;

    static final String PRODUCTO = """
            {"nombre":"Producto de prueba","descripcion":"desc","categoria":"Pruebas","precio":1000,"stock":5}""";

    @Test
    void laRaizPermiteDescubrirLasDosVersiones() throws Exception {
        mvc.perform(get("/api").accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._links.self.href").exists())
                .andExpect(jsonPath("$._links.v1-productos.href", endsWith("/api/v1/productos")))
                .andExpect(jsonPath("$._links.v1-ordenes.href", containsString("/api/v1/ordenes")))
                // link plantilla (RFC 6570): el cliente puede agregar ?estado=
                .andExpect(jsonPath("$._links.v1-ordenes.templated").value(true))
                .andExpect(jsonPath("$._links.v2-productos.href", containsString("/api/v2/productos")));
    }

    @Test
    void v1DevuelveColeccionHalConLinks() throws Exception {
        mvc.perform(get("/api/v1/productos"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-API-Version", "1"))
                .andExpect(header().exists("X-Request-Id"))
                .andExpect(jsonPath("$._embedded.productos", hasSize(greaterThanOrEqualTo(8))))
                .andExpect(jsonPath("$._embedded.productos[0]._links.self.href").exists())
                .andExpect(jsonPath("$._embedded.productos[0]._links.version-2.href", containsString("/api/v2/productos/")))
                .andExpect(jsonPath("$._links.crear.href").exists())
                // v1 es plana: no trae los campos de v2
                .andExpect(jsonPath("$._embedded.productos[0].categoria").doesNotExist());
    }

    @Test
    void v2PaginaYCambiaLaRepresentacion() throws Exception {
        mvc.perform(get("/api/v2/productos").param("size", "3"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-API-Version", "2"))
                .andExpect(jsonPath("$._embedded.productos", hasSize(3)))
                .andExpect(jsonPath("$.page.size").value(3))
                .andExpect(jsonPath("$._links.next.href").exists())
                .andExpect(jsonPath("$._links.last.href").exists())
                .andExpect(jsonPath("$._embedded.productos[0].precio.moneda").value("COP"))
                .andExpect(jsonPath("$._embedded.productos[0].inventario.disponible").value(true));

        mvc.perform(get("/api/v2/productos").param("categoria", "oficina"))
                .andExpect(jsonPath("$._embedded.productos[*].categoria", everyItem(is("Oficina"))));
    }

    @Test
    void crudDeProductoConLocation() throws Exception {
        String location = mvc.perform(post("/api/v1/productos").contentType(MediaType.APPLICATION_JSON).content(PRODUCTO))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$._links.ordenar.href").exists())
                .andReturn().getResponse().getHeader("Location");

        mvc.perform(put(location).contentType(MediaType.APPLICATION_JSON).content(PRODUCTO.replace("\"stock\":5", "\"stock\":0")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stock").value(0))
                // sin stock ya no se ofrece el link para ordenar
                .andExpect(jsonPath("$._links.ordenar").doesNotExist());

        mvc.perform(delete(location)).andExpect(status().isNoContent());
        mvc.perform(get(location)).andExpect(status().isNotFound());
    }

    @Test
    void losLinksDeLaOrdenSiguenSuEstado() throws Exception {
        String producto = mvc.perform(post("/api/v1/productos").contentType(MediaType.APPLICATION_JSON).content(PRODUCTO))
                .andReturn().getResponse().getContentAsString();
        Integer productoId = JsonPath.read(producto, "$.id");

        String orden = mvc.perform(post("/api/v1/ordenes").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cliente\":\"Ana\",\"productoId\":" + productoId + ",\"cantidad\":2}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("CREADA"))
                .andExpect(jsonPath("$.total").value(2000))
                .andExpect(jsonPath("$._links.pagar.href").exists())
                .andExpect(jsonPath("$._links.cancelar.href").exists())
                .andExpect(jsonPath("$._links.enviar").doesNotExist())
                .andReturn().getResponse().getContentAsString();
        String pagar = JsonPath.read(orden, "$._links.pagar.href");

        String pagada = mvc.perform(post(pagar))
                .andExpect(jsonPath("$.estado").value("PAGADA"))
                .andExpect(jsonPath("$._links.pagar").doesNotExist())
                .andExpect(jsonPath("$._links.enviar.href").exists())
                .andReturn().getResponse().getContentAsString();
        String enviar = JsonPath.read(pagada, "$._links.enviar.href");
        String cancelar = JsonPath.read(pagada, "$._links.cancelar.href");

        mvc.perform(post(enviar))
                .andExpect(jsonPath("$.estado").value("ENVIADA"))
                .andExpect(jsonPath("$._links.cancelar").doesNotExist());

        mvc.perform(post(cancelar))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail", containsString("ENVIADA")));

        mvc.perform(get("/api/v1/productos/" + productoId)).andExpect(jsonPath("$.stock").value(3));
    }

    @Test
    void erroresEnFormatoProblemDetail() throws Exception {
        mvc.perform(post("/api/v1/productos").contentType(MediaType.APPLICATION_JSON).content("{\"nombre\":\"\",\"precio\":-1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Datos inválidos"))
                .andExpect(jsonPath("$.errores.nombre").exists())
                .andExpect(jsonPath("$.errores.precio").exists());

        mvc.perform(get("/api/v2/productos/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("No existe producto con id 999999."));

        mvc.perform(post("/api/v1/ordenes").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cliente\":\"Ana\",\"productoId\":1,\"cantidad\":100}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail", containsString("Stock insuficiente")));
    }

    @Test
    void exponeMetricasParaPrometheus() throws Exception {
        mvc.perform(get("/api/v1/productos"));
        mvc.perform(get("/actuator/prometheus"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("http_server_requests_seconds")))
                .andExpect(content().string(containsString("application=\"parcial-api\"")));
    }
}

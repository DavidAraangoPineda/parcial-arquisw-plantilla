package com.udea.parcial.web.v2;

import com.udea.parcial.dominio.Producto;
import org.springframework.hateoas.server.core.Relation;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Representación v2. Cambios respecto a v1 (por eso es una versión nueva y no se rompe a los
 * clientes de v1): agrega descripción, categoría y fecha; el precio pasa a ser un objeto con
 * moneda, y el stock se agrupa en "inventario" con el indicador "disponible".
 */
@Relation(itemRelation = "producto", collectionRelation = "productos")
public record ProductoV2(Long id, String nombre, String descripcion, String categoria,
                         Precio precio, Inventario inventario, Instant creadoEn) {

    public record Precio(BigDecimal valor, String moneda) {}

    public record Inventario(int stock, boolean disponible) {}

    public static ProductoV2 de(Producto p) {
        return new ProductoV2(p.getId(), p.getNombre(), p.getDescripcion(), p.getCategoria(),
                new Precio(p.getPrecio(), "COP"), new Inventario(p.getStock(), p.getStock() > 0), p.getCreadoEn());
    }
}

package com.udea.parcial.web.v1;

import com.udea.parcial.dominio.Producto;
import org.springframework.hateoas.server.core.Relation;

import java.math.BigDecimal;

/** Representación v1: plana y mínima. */
@Relation(itemRelation = "producto", collectionRelation = "productos")
public record ProductoV1(Long id, String nombre, BigDecimal precio, Integer stock) {

    public static ProductoV1 de(Producto p) {
        return new ProductoV1(p.getId(), p.getNombre(), p.getPrecio(), p.getStock());
    }
}

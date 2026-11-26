package com.udea.parcial.web.v1;

import com.udea.parcial.dominio.EstadoOrden;
import com.udea.parcial.dominio.Orden;
import org.springframework.hateoas.server.core.Relation;

import java.math.BigDecimal;
import java.time.Instant;

@Relation(itemRelation = "orden", collectionRelation = "ordenes")
public record OrdenV1(Long id, String cliente, Long productoId, String producto, Integer cantidad,
                      BigDecimal total, EstadoOrden estado, Instant creadaEn, Instant actualizadaEn) {

    public static OrdenV1 de(Orden o) {
        return new OrdenV1(o.getId(), o.getCliente(), o.getProducto().getId(), o.getProducto().getNombre(),
                o.getCantidad(), o.getTotal(), o.getEstado(), o.getCreadaEn(), o.getActualizadaEn());
    }
}

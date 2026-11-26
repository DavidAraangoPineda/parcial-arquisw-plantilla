package com.udea.parcial.web.v1;

import com.udea.parcial.dominio.EstadoOrden;
import com.udea.parcial.dominio.Orden;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.stereotype.Component;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

/**
 * HATEOAS como motor del estado: solo se publican los links de las transiciones válidas.
 * Una orden CREADA trae "pagar" y "cancelar"; una PAGADA trae "enviar" y "cancelar";
 * una ENVIADA o CANCELADA no trae acciones.
 */
@Component
public class OrdenAssemblerV1 implements RepresentationModelAssembler<Orden, EntityModel<OrdenV1>> {

    @Override
    public EntityModel<OrdenV1> toModel(Orden o) {
        EstadoOrden estado = o.getEstado();
        EntityModel<OrdenV1> modelo = EntityModel.of(OrdenV1.de(o),
                linkTo(methodOn(OrdenControllerV1.class).obtener(o.getId())).withSelfRel(),
                linkTo(methodOn(OrdenControllerV1.class).listar(null)).withRel("ordenes"),
                linkTo(methodOn(ProductoControllerV1.class).obtener(o.getProducto().getId())).withRel("producto"));

        modelo.addIf(estado.puedePagarse(),
                () -> linkTo(methodOn(OrdenControllerV1.class).pagar(o.getId())).withRel("pagar"));
        modelo.addIf(estado.puedeEnviarse(),
                () -> linkTo(methodOn(OrdenControllerV1.class).enviar(o.getId())).withRel("enviar"));
        modelo.addIf(estado.puedeCancelarse(),
                () -> linkTo(methodOn(OrdenControllerV1.class).cancelar(o.getId())).withRel("cancelar"));
        return modelo;
    }
}

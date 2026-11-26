package com.udea.parcial.web.v1;

import com.udea.parcial.dominio.Producto;
import com.udea.parcial.web.v2.ProductoControllerV2;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.stereotype.Component;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@Component
public class ProductoAssemblerV1 implements RepresentationModelAssembler<Producto, EntityModel<ProductoV1>> {

    @Override
    public EntityModel<ProductoV1> toModel(Producto p) {
        EntityModel<ProductoV1> modelo = EntityModel.of(ProductoV1.de(p),
                linkTo(methodOn(ProductoControllerV1.class).obtener(p.getId())).withSelfRel(),
                linkTo(methodOn(ProductoControllerV1.class).listar()).withRel("productos"),
                linkTo(methodOn(ProductoControllerV1.class).actualizar(p.getId(), null)).withRel("actualizar"),
                linkTo(methodOn(ProductoControllerV1.class).eliminar(p.getId())).withRel("eliminar"),
                // El mismo recurso en la versión nueva de la API.
                linkTo(methodOn(ProductoControllerV2.class).obtener(p.getId())).withRel("version-2"));
        if (p.getStock() > 0) {
            modelo.add(linkTo(methodOn(OrdenControllerV1.class).crear(null)).withRel("ordenar"));
        }
        return modelo;
    }
}

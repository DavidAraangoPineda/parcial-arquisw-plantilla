package com.udea.parcial.web.v2;

import com.udea.parcial.dominio.Producto;
import com.udea.parcial.web.v1.OrdenControllerV1;
import com.udea.parcial.web.v1.ProductoControllerV1;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.stereotype.Component;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@Component
public class ProductoAssemblerV2 implements RepresentationModelAssembler<Producto, EntityModel<ProductoV2>> {

    @Override
    public EntityModel<ProductoV2> toModel(Producto p) {
        EntityModel<ProductoV2> modelo = EntityModel.of(ProductoV2.de(p),
                linkTo(methodOn(ProductoControllerV2.class).obtener(p.getId())).withSelfRel(),
                linkTo(methodOn(ProductoControllerV2.class).listar(null, null)).withRel("productos"),
                linkTo(methodOn(ProductoControllerV2.class).listar(p.getCategoria(), null)).withRel("misma-categoria"),
                linkTo(methodOn(ProductoControllerV2.class).actualizar(p.getId(), null)).withRel("actualizar"),
                linkTo(methodOn(ProductoControllerV2.class).eliminar(p.getId())).withRel("eliminar"),
                linkTo(methodOn(ProductoControllerV1.class).obtener(p.getId())).withRel("version-1"));
        modelo.addIf(p.getStock() > 0, () -> linkTo(methodOn(OrdenControllerV1.class).crear(null)).withRel("ordenar"));
        return modelo;
    }
}

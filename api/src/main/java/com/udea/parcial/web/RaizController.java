package com.udea.parcial.web;

import com.udea.parcial.web.v1.OrdenControllerV1;
import com.udea.parcial.web.v1.ProductoControllerV1;
import com.udea.parcial.web.v2.ProductoControllerV2;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

/**
 * Punto de entrada de la API (HATEOAS): el cliente solo necesita conocer /api y descubre
 * el resto siguiendo los links, incluidas las dos versiones.
 */
@RestController
public class RaizController {

    @GetMapping("/api")
    public RepresentationModel<?> raiz() {
        RepresentationModel<?> raiz = new RepresentationModel<>();
        raiz.add(linkTo(methodOn(RaizController.class).raiz()).withSelfRel());
        raiz.add(linkTo(methodOn(ProductoControllerV1.class).listar()).withRel("v1-productos"));
        raiz.add(linkTo(methodOn(OrdenControllerV1.class).listar(null)).withRel("v1-ordenes"));
        raiz.add(linkTo(methodOn(ProductoControllerV2.class).listar(null, null)).withRel("v2-productos"));
        raiz.add(Link.of(linkTo(RaizController.class).toUriComponentsBuilder().replacePath("/swagger-ui.html").toUriString(), "documentacion"));
        raiz.add(Link.of(linkTo(RaizController.class).toUriComponentsBuilder().replacePath("/actuator/health").toUriString(), "salud"));
        return raiz;
    }
}

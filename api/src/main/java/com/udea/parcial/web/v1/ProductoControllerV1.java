package com.udea.parcial.web.v1;

import com.udea.parcial.servicio.ProductoService;
import com.udea.parcial.web.ProductoRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.IanaLinkRelations;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

/** Versión 1 (por URI): lista completa sin paginar y representación plana. */
@RestController
@RequestMapping("/api/v1/productos")
@Tag(name = "Productos v1")
public class ProductoControllerV1 {

    private final ProductoService servicio;
    private final ProductoAssemblerV1 assembler;

    public ProductoControllerV1(ProductoService servicio, ProductoAssemblerV1 assembler) {
        this.servicio = servicio;
        this.assembler = assembler;
    }

    @GetMapping
    @Operation(summary = "Lista todos los productos")
    public CollectionModel<EntityModel<ProductoV1>> listar() {
        return assembler.toCollectionModel(servicio.listar())
                .add(linkTo(methodOn(ProductoControllerV1.class).listar()).withSelfRel())
                .add(linkTo(methodOn(ProductoControllerV1.class).crear(null)).withRel("crear"));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtiene un producto")
    public EntityModel<ProductoV1> obtener(@PathVariable Long id) {
        return assembler.toModel(servicio.obtener(id));
    }

    @PostMapping
    @Operation(summary = "Crea un producto")
    public ResponseEntity<EntityModel<ProductoV1>> crear(@Valid @RequestBody ProductoRequest request) {
        EntityModel<ProductoV1> modelo = assembler.toModel(servicio.crear(request));
        return ResponseEntity.created(modelo.getRequiredLink(IanaLinkRelations.SELF).toUri()).body(modelo);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualiza un producto")
    public EntityModel<ProductoV1> actualizar(@PathVariable Long id, @Valid @RequestBody ProductoRequest request) {
        return assembler.toModel(servicio.actualizar(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Elimina un producto sin órdenes")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        servicio.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}

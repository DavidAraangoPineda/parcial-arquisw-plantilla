package com.udea.parcial.web.v2;

import com.udea.parcial.dominio.Producto;
import com.udea.parcial.servicio.ProductoService;
import com.udea.parcial.web.ProductoRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.IanaLinkRelations;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

/**
 * Versión 2 (por URI). Diferencias con v1: paginación con links first/prev/next/last,
 * filtro por categoría, ordenamiento y una representación más rica (ver ProductoV2).
 */
@RestController
@RequestMapping("/api/v2/productos")
@Tag(name = "Productos v2")
public class ProductoControllerV2 {

    private final ProductoService servicio;
    private final ProductoAssemblerV2 assembler;
    private final PagedResourcesAssembler<Producto> paginador;

    public ProductoControllerV2(ProductoService servicio, ProductoAssemblerV2 assembler,
                                PagedResourcesAssembler<Producto> paginador) {
        this.servicio = servicio;
        this.assembler = assembler;
        this.paginador = paginador;
    }

    @GetMapping
    @Operation(summary = "Lista productos paginados (page, size, sort) y filtrados por categoría")
    public PagedModel<EntityModel<ProductoV2>> listar(
            @Parameter(description = "Categoría exacta, sin distinguir mayúsculas") @RequestParam(required = false) String categoria,
            @ParameterObject @PageableDefault(size = 5, sort = "id", direction = Sort.Direction.ASC) Pageable pageable) {
        PagedModel<EntityModel<ProductoV2>> pagina = paginador.toModel(servicio.listar(categoria, pageable), assembler);
        pagina.add(linkTo(methodOn(ProductoControllerV2.class).crear(null)).withRel("crear"));
        return pagina;
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtiene un producto")
    public EntityModel<ProductoV2> obtener(@PathVariable Long id) {
        return assembler.toModel(servicio.obtener(id));
    }

    @PostMapping
    @Operation(summary = "Crea un producto")
    public ResponseEntity<EntityModel<ProductoV2>> crear(@Valid @RequestBody ProductoRequest request) {
        EntityModel<ProductoV2> modelo = assembler.toModel(servicio.crear(request));
        return ResponseEntity.created(modelo.getRequiredLink(IanaLinkRelations.SELF).toUri()).body(modelo);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualiza un producto")
    public EntityModel<ProductoV2> actualizar(@PathVariable Long id, @Valid @RequestBody ProductoRequest request) {
        return assembler.toModel(servicio.actualizar(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Elimina un producto sin órdenes")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        servicio.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}

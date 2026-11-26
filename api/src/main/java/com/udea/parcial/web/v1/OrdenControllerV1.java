package com.udea.parcial.web.v1;

import com.udea.parcial.dominio.EstadoOrden;
import com.udea.parcial.servicio.OrdenService;
import com.udea.parcial.web.OrdenRequest;
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

@RestController
@RequestMapping("/api/v1/ordenes")
@Tag(name = "Órdenes v1")
public class OrdenControllerV1 {

    private final OrdenService servicio;
    private final OrdenAssemblerV1 assembler;

    public OrdenControllerV1(OrdenService servicio, OrdenAssemblerV1 assembler) {
        this.servicio = servicio;
        this.assembler = assembler;
    }

    @GetMapping
    @Operation(summary = "Lista las órdenes, opcionalmente filtradas por estado")
    public CollectionModel<EntityModel<OrdenV1>> listar(@RequestParam(required = false) EstadoOrden estado) {
        return assembler.toCollectionModel(servicio.listar(estado))
                .add(linkTo(methodOn(OrdenControllerV1.class).listar(estado)).withSelfRel())
                .add(linkTo(methodOn(OrdenControllerV1.class).crear(null)).withRel("crear"));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtiene una orden con los links de las acciones permitidas")
    public EntityModel<OrdenV1> obtener(@PathVariable Long id) {
        return assembler.toModel(servicio.obtener(id));
    }

    @PostMapping
    @Operation(summary = "Crea una orden y descuenta el stock")
    public ResponseEntity<EntityModel<OrdenV1>> crear(@Valid @RequestBody OrdenRequest request) {
        EntityModel<OrdenV1> modelo = assembler.toModel(servicio.crear(request));
        return ResponseEntity.created(modelo.getRequiredLink(IanaLinkRelations.SELF).toUri()).body(modelo);
    }

    @PostMapping("/{id}/pagar")
    @Operation(summary = "CREADA -> PAGADA")
    public EntityModel<OrdenV1> pagar(@PathVariable Long id) {
        return assembler.toModel(servicio.pagar(id));
    }

    @PostMapping("/{id}/enviar")
    @Operation(summary = "PAGADA -> ENVIADA")
    public EntityModel<OrdenV1> enviar(@PathVariable Long id) {
        return assembler.toModel(servicio.enviar(id));
    }

    @PostMapping("/{id}/cancelar")
    @Operation(summary = "CREADA o PAGADA -> CANCELADA (devuelve el stock)")
    public EntityModel<OrdenV1> cancelar(@PathVariable Long id) {
        return assembler.toModel(servicio.cancelar(id));
    }
}

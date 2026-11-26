package com.udea.parcial.servicio;

import com.udea.parcial.dominio.EstadoOrden;
import com.udea.parcial.dominio.Orden;
import com.udea.parcial.dominio.Producto;
import com.udea.parcial.dominio.RecursoNoEncontradoException;
import com.udea.parcial.repositorio.OrdenRepository;
import com.udea.parcial.web.OrdenRequest;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.function.Consumer;

@Service
@Transactional
public class OrdenService {

    private static final Logger log = LoggerFactory.getLogger(OrdenService.class);

    private final OrdenRepository ordenes;
    private final ProductoService productos;
    private final MeterRegistry metricas;

    public OrdenService(OrdenRepository ordenes, ProductoService productos, MeterRegistry metricas) {
        this.ordenes = ordenes;
        this.productos = productos;
        this.metricas = metricas;
    }

    @Transactional(readOnly = true)
    public List<Orden> listar(EstadoOrden estado) {
        return estado == null ? ordenes.findAll() : ordenes.findByEstadoOrderByIdDesc(estado);
    }

    @Transactional(readOnly = true)
    public Orden obtener(Long id) {
        return ordenes.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("orden", id));
    }

    public Orden crear(OrdenRequest r) {
        Producto producto = productos.obtener(r.productoId());
        Orden orden = ordenes.save(new Orden(r.cliente(), producto, r.cantidad()));
        registrar("creada", orden);
        return orden;
    }

    public Orden pagar(Long id) { return transicion(id, Orden::pagar, "pagada"); }

    public Orden enviar(Long id) { return transicion(id, Orden::enviar, "enviada"); }

    public Orden cancelar(Long id) { return transicion(id, Orden::cancelar, "cancelada"); }

    private Orden transicion(Long id, Consumer<Orden> accion, String evento) {
        Orden orden = obtener(id);
        accion.accept(orden);
        Orden guardada = ordenes.save(orden);
        registrar(evento, guardada);
        return guardada;
    }

    /** Métrica de negocio (parcial_ordenes_total en Prometheus) y log para Loki. */
    private void registrar(String evento, Orden orden) {
        metricas.counter("parcial.ordenes", "evento", evento).increment();
        log.info("Orden {} id={} producto={} cantidad={} total={}",
                evento, orden.getId(), orden.getProducto().getId(), orden.getCantidad(), orden.getTotal());
    }
}

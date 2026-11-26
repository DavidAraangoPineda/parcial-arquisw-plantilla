package com.udea.parcial.dominio;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Recurso secundario con estados. Sirve para mostrar HATEOAS "de verdad": los links de acciones
 * (pagar, enviar, cancelar) cambian según el estado de la orden.
 */
@Entity
@Table(name = "orden")
public class Orden {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String cliente;

    @ManyToOne(optional = false)
    @JoinColumn(name = "producto_id")
    private Producto producto;

    @Column(nullable = false)
    private Integer cantidad;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal total;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoOrden estado;

    @Column(nullable = false, updatable = false)
    private Instant creadaEn;

    private Instant actualizadaEn;

    @Version
    private Long version;

    protected Orden() {}

    public Orden(String cliente, Producto producto, int cantidad) {
        producto.descontarStock(cantidad);
        this.cliente = cliente;
        this.producto = producto;
        this.cantidad = cantidad;
        this.total = producto.getPrecio().multiply(BigDecimal.valueOf(cantidad));
        this.estado = EstadoOrden.CREADA;
    }

    @PrePersist
    void alCrear() {
        creadaEn = Instant.now();
        actualizadaEn = creadaEn;
    }

    @PreUpdate
    void alActualizar() {
        actualizadaEn = Instant.now();
    }

    public void pagar() {
        exigir(estado.puedePagarse(), "pagar");
        estado = EstadoOrden.PAGADA;
    }

    public void enviar() {
        exigir(estado.puedeEnviarse(), "enviar");
        estado = EstadoOrden.ENVIADA;
    }

    public void cancelar() {
        exigir(estado.puedeCancelarse(), "cancelar");
        producto.devolverStock(cantidad);
        estado = EstadoOrden.CANCELADA;
    }

    private void exigir(boolean permitido, String accion) {
        if (!permitido) {
            throw new ReglaNegocioException("No se puede " + accion + " la orden " + id + " porque está " + estado + ".");
        }
    }

    public Long getId() { return id; }
    public String getCliente() { return cliente; }
    public Producto getProducto() { return producto; }
    public Integer getCantidad() { return cantidad; }
    public BigDecimal getTotal() { return total; }
    public EstadoOrden getEstado() { return estado; }
    public Instant getCreadaEn() { return creadaEn; }
    public Instant getActualizadaEn() { return actualizadaEn; }
}

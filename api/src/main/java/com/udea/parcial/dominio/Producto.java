package com.udea.parcial.dominio;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Recurso principal de ejemplo. Para el parcial: renombrar y cambiar campos según el enunciado.
 */
@Entity
@Table(name = "producto")
public class Producto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String nombre;

    @Column(length = 500)
    private String descripcion;

    @Column(nullable = false, length = 60)
    private String categoria;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal precio;

    @Column(nullable = false)
    private Integer stock;

    @Column(nullable = false, updatable = false)
    private Instant creadoEn;

    /** Bloqueo optimista: dos actualizaciones simultáneas no se pisan. */
    @Version
    private Long version;

    protected Producto() {}

    public Producto(String nombre, String descripcion, String categoria, BigDecimal precio, Integer stock) {
        actualizar(nombre, descripcion, categoria, precio, stock);
    }

    @PrePersist
    void alCrear() {
        creadoEn = Instant.now();
    }

    public void actualizar(String nombre, String descripcion, String categoria, BigDecimal precio, Integer stock) {
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.categoria = categoria;
        this.precio = precio;
        this.stock = stock;
    }

    public void descontarStock(int cantidad) {
        if (cantidad > stock) {
            throw new ReglaNegocioException("Stock insuficiente de '" + nombre + "': hay " + stock + " y se piden " + cantidad + ".");
        }
        stock -= cantidad;
    }

    public void devolverStock(int cantidad) {
        stock += cantidad;
    }

    public Long getId() { return id; }
    public String getNombre() { return nombre; }
    public String getDescripcion() { return descripcion; }
    public String getCategoria() { return categoria; }
    public BigDecimal getPrecio() { return precio; }
    public Integer getStock() { return stock; }
    public Instant getCreadoEn() { return creadoEn; }
}

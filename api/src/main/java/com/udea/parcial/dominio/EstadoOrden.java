package com.udea.parcial.dominio;

/**
 * Máquina de estados de la orden. HATEOAS publica como links solo las transiciones válidas:
 * CREADA -> PAGADA -> ENVIADA, y CREADA/PAGADA -> CANCELADA.
 */
public enum EstadoOrden {
    CREADA, PAGADA, ENVIADA, CANCELADA;

    public boolean puedePagarse() { return this == CREADA; }
    public boolean puedeEnviarse() { return this == PAGADA; }
    public boolean puedeCancelarse() { return this == CREADA || this == PAGADA; }
}

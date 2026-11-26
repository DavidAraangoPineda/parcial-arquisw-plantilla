package com.udea.parcial.dominio;

/** El recurso pedido no existe. Se responde con 404 Not Found. */
public class RecursoNoEncontradoException extends RuntimeException {
    public RecursoNoEncontradoException(String recurso, Object id) {
        super("No existe " + recurso + " con id " + id + ".");
    }
}

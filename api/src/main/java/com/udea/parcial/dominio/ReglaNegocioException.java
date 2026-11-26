package com.udea.parcial.dominio;

/** Se viola una regla del negocio. Se responde con 409 Conflict. */
public class ReglaNegocioException extends RuntimeException {
    public ReglaNegocioException(String mensaje) {
        super(mensaje);
    }
}

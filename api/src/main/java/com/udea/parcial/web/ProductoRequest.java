package com.udea.parcial.web;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

/** Cuerpo para crear o actualizar un producto. Lo comparten v1 y v2. */
public record ProductoRequest(
        @NotBlank @Size(max = 120) String nombre,
        @Size(max = 500) String descripcion,
        @NotBlank @Size(max = 60) String categoria,
        @NotNull @DecimalMin(value = "0.01") @Digits(integer = 10, fraction = 2) BigDecimal precio,
        @NotNull @PositiveOrZero Integer stock) {
}

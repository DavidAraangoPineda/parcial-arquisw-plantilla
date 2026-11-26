package com.udea.parcial.web;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record OrdenRequest(
        @NotBlank String cliente,
        @NotNull Long productoId,
        @NotNull @Min(1) @Max(100) Integer cantidad) {
}

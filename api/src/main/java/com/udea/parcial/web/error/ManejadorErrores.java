package com.udea.parcial.web.error;

import com.udea.parcial.dominio.RecursoNoEncontradoException;
import com.udea.parcial.dominio.ReglaNegocioException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.mapping.PropertyReferenceException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Todos los errores salen en formato RFC 9457 (application/problem+json), con un mensaje claro.
 * 400 datos inválidos, 404 no existe, 409 regla de negocio o conflicto, 500 error inesperado.
 */
@RestControllerAdvice
public class ManejadorErrores extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ManejadorErrores.class);

    @ExceptionHandler(RecursoNoEncontradoException.class)
    ProblemDetail noEncontrado(RecursoNoEncontradoException e) {
        return problema(HttpStatus.NOT_FOUND, "Recurso no encontrado", e.getMessage());
    }

    @ExceptionHandler(ReglaNegocioException.class)
    ProblemDetail reglaNegocio(ReglaNegocioException e) {
        log.warn("Regla de negocio: {}", e.getMessage());
        return problema(HttpStatus.CONFLICT, "Regla de negocio", e.getMessage());
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    ProblemDetail concurrencia(OptimisticLockingFailureException e) {
        return problema(HttpStatus.CONFLICT, "Conflicto de concurrencia",
                "Otro usuario modificó el recurso al mismo tiempo. Consulte de nuevo e intente otra vez.");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ProblemDetail integridad(DataIntegrityViolationException e) {
        return problema(HttpStatus.CONFLICT, "Conflicto de datos", "La operación viola una restricción de la base de datos.");
    }

    @ExceptionHandler(PropertyReferenceException.class)
    ProblemDetail ordenInvalido(PropertyReferenceException e) {
        return problema(HttpStatus.BAD_REQUEST, "Parámetro inválido", "No se puede ordenar por '" + e.getPropertyName() + "'.");
    }

    @ExceptionHandler(Exception.class)
    ProblemDetail inesperado(Exception e) {
        log.error("Error inesperado", e);
        return problema(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno", "Ocurrió un error inesperado.");
    }

    /** 400 con el detalle de cada campo inválido. */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException e, HttpHeaders headers,
                                                                  HttpStatusCode status, WebRequest request) {
        Map<String, String> errores = new LinkedHashMap<>();
        e.getBindingResult().getFieldErrors().forEach(f -> errores.put(f.getField(), f.getDefaultMessage()));
        ProblemDetail p = problema(HttpStatus.BAD_REQUEST, "Datos inválidos", "Revise los campos indicados en 'errores'.");
        p.setProperty("errores", errores);
        return ResponseEntity.badRequest().body(p);
    }

    private static ProblemDetail problema(HttpStatus status, String titulo, String detalle) {
        ProblemDetail p = ProblemDetail.forStatusAndDetail(status, detalle);
        p.setTitle(titulo);
        return p;
    }
}

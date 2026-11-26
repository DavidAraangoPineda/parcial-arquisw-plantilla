package com.udea.parcial.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Observabilidad de cada petición:
 * - asigna un X-Request-Id (o reutiliza el que mande el cliente) y lo pone en el MDC, así aparece
 *   en todos los logs de esa petición y se puede buscar en Grafana/Loki;
 * - agrega el encabezado X-API-Version según la ruta (/api/v1 o /api/v2);
 * - escribe una línea de log con método, ruta, estado y duración.
 */
@Component
public class RegistroPeticionesFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger("http");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String requestId = request.getHeader("X-Request-Id");
        if (requestId == null || requestId.isBlank()) {
            requestId = UUID.randomUUID().toString();
        }
        String uri = request.getRequestURI();
        String version = uri.startsWith("/api/v2") ? "2" : uri.startsWith("/api/v1") ? "1" : null;

        MDC.put("requestId", requestId);
        response.setHeader("X-Request-Id", requestId);
        if (version != null) {
            MDC.put("apiVersion", version);
            response.setHeader("X-API-Version", version);
        }
        long inicio = System.nanoTime();
        try {
            chain.doFilter(request, response);
        } finally {
            long ms = (System.nanoTime() - inicio) / 1_000_000;
            log.info("{} {} -> {} ({} ms)", request.getMethod(), uri, response.getStatus(), ms);
            MDC.clear();
        }
    }

    /** Las consultas de Prometheus a /actuator llenarían el log de ruido. */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return request.getRequestURI().startsWith("/actuator");
    }
}

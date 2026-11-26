package com.udea.parcial.config;

import com.udea.parcial.dominio.Producto;
import com.udea.parcial.repositorio.ProductoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/** Carga datos de ejemplo solo si la tabla está vacía. Para el parcial: cambiar por datos del enunciado. */
@Component
public class DatosIniciales implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DatosIniciales.class);

    private final ProductoRepository productos;

    public DatosIniciales(ProductoRepository productos) {
        this.productos = productos;
    }

    @Override
    public void run(String... args) {
        if (productos.count() > 0) {
            return;
        }
        productos.saveAll(List.of(
                new Producto("Portátil 14\"", "16 GB RAM, 512 GB SSD", "Tecnología", new BigDecimal("2899000"), 10),
                new Producto("Mouse inalámbrico", "Receptor USB", "Tecnología", new BigDecimal("69000"), 50),
                new Producto("Teclado mecánico", "Switches rojos", "Tecnología", new BigDecimal("259000"), 20),
                new Producto("Monitor 27\"", "IPS 75 Hz", "Tecnología", new BigDecimal("899000"), 8),
                new Producto("Silla ergonómica", "Soporte lumbar", "Oficina", new BigDecimal("749000"), 5),
                new Producto("Escritorio", "120 x 60 cm", "Oficina", new BigDecimal("520000"), 4),
                new Producto("Cuaderno", "100 hojas cuadriculado", "Papelería", new BigDecimal("8500"), 200),
                new Producto("Bolígrafos x10", "Tinta negra", "Papelería", new BigDecimal("12000"), 150)));
        log.info("Datos iniciales cargados: {} productos", productos.count());
    }
}

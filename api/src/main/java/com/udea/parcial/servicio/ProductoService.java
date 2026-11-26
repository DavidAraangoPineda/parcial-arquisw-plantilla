package com.udea.parcial.servicio;

import com.udea.parcial.dominio.Producto;
import com.udea.parcial.dominio.RecursoNoEncontradoException;
import com.udea.parcial.dominio.ReglaNegocioException;
import com.udea.parcial.repositorio.OrdenRepository;
import com.udea.parcial.repositorio.ProductoRepository;
import com.udea.parcial.web.ProductoRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Lógica del recurso. Los controladores v1 y v2 comparten este servicio: solo cambia la representación. */
@Service
@Transactional
public class ProductoService {

    private static final Logger log = LoggerFactory.getLogger(ProductoService.class);

    private final ProductoRepository productos;
    private final OrdenRepository ordenes;

    public ProductoService(ProductoRepository productos, OrdenRepository ordenes) {
        this.productos = productos;
        this.ordenes = ordenes;
    }

    @Transactional(readOnly = true)
    public List<Producto> listar() {
        return productos.findAll();
    }

    @Transactional(readOnly = true)
    public Page<Producto> listar(String categoria, Pageable pageable) {
        return categoria == null || categoria.isBlank()
                ? productos.findAll(pageable)
                : productos.findByCategoriaIgnoreCase(categoria.trim(), pageable);
    }

    @Transactional(readOnly = true)
    public Producto obtener(Long id) {
        return productos.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("producto", id));
    }

    public Producto crear(ProductoRequest r) {
        Producto p = productos.save(new Producto(r.nombre(), r.descripcion(), r.categoria(), r.precio(), r.stock()));
        log.info("Producto creado id={} nombre='{}'", p.getId(), p.getNombre());
        return p;
    }

    public Producto actualizar(Long id, ProductoRequest r) {
        Producto p = obtener(id);
        p.actualizar(r.nombre(), r.descripcion(), r.categoria(), r.precio(), r.stock());
        log.info("Producto actualizado id={}", id);
        return productos.save(p);
    }

    public void eliminar(Long id) {
        Producto p = obtener(id);
        if (ordenes.existsByProductoId(id)) {
            throw new ReglaNegocioException("El producto " + id + " tiene órdenes asociadas y no se puede eliminar.");
        }
        productos.delete(p);
        log.info("Producto eliminado id={}", id);
    }
}

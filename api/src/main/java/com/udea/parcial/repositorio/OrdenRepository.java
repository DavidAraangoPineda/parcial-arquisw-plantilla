package com.udea.parcial.repositorio;

import com.udea.parcial.dominio.EstadoOrden;
import com.udea.parcial.dominio.Orden;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrdenRepository extends JpaRepository<Orden, Long> {

    List<Orden> findByEstadoOrderByIdDesc(EstadoOrden estado);

    boolean existsByProductoId(Long productoId);
}

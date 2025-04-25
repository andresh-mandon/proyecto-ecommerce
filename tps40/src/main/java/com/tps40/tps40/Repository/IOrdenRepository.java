package com.tps40.tps40.Repository;
import com.tps40.tps40.Entities.Orden;
import com.tps40.tps40.Entities.Orden.EstadoOrden;
import com.tps40.tps40.Entities.Productos;
import com.tps40.tps40.Entities.Usuarios;


import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;// Cambia esta importación

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.List;

@Repository
public interface IOrdenRepository extends JpaRepository<Orden, Long> {
    List<Orden> findByUsuario(Usuarios usuario);
      
    // Método corregido para usar el enum
    long countByEstado(EstadoOrden estado);
    
    @Query("SELECT COALESCE(SUM(o.total), 0) FROM Orden o WHERE o.fechaCreacion >= :inicio AND o.fechaCreacion < :fin AND o.estado = 'ENTREGADO'")
    BigDecimal sumVentasEntregadasEntreFechas(
            @Param("inicio") LocalDateTime inicio,
            @Param("fin") LocalDateTime fin);

            @Query("SELECT COALESCE(SUM(o.total), 0) FROM Orden o WHERE YEAR(o.fechaCreacion) = :year AND MONTH(o.fechaCreacion) = :month AND o.estado = 'ENTREGADO'")
            BigDecimal sumVentasEntregadasByMonthAndYear(
                    @Param("year") int year,
                    @Param("month") int month);


    Page<Orden> findByEstadoOrderByFechaCreacionDesc(EstadoOrden estado, Pageable pageable);

    Page<Orden> findAllByOrderByFechaCreacionDesc(Pageable pageable);


    // Métodos adicionales útiles
    List<Orden> findByUsuario_Id(Long usuarioId);


    @Query("SELECT o FROM Orden o WHERE o.fechaCreacion BETWEEN :startDate AND :endDate")
    List<Orden> findBetweenDates(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    @Query("SELECT o FROM Orden o WHERE o.usuario.id = :usuarioId AND o.estado = :estado")
    List<Orden> findByUsuarioAndEstado(
            @Param("usuarioId") Long usuarioId,
            @Param("estado") EstadoOrden estado);

            List<Orden> findByEstado(Orden.EstadoOrden estado);
            List<Orden> findByFechaCreacionBetween(LocalDateTime fechaInicio, LocalDateTime fechaFin);
         List<Orden> findByUsuarioId(Long usuarioId);
         List<Orden> findByUsuarioOrderByFechaCreacionDesc(Usuarios usuario);

}

package com.tps40.tps40.Repository;
import com.tps40.tps40.Entities.Productos;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface IProductosRepository extends JpaRepository<Productos, Long>  {
    // Búsqueda por nombre (contiene)
    List<Productos> findByNombreContainingIgnoreCase(String nombre);

    // Búsqueda por categoría
    List<Productos> findByCategoriaIdIn(List<Long> categoriaIds);

    // Búsqueda por rango de precios
    @Query("SELECT p FROM Productos p WHERE p.precio BETWEEN :min AND :max")
    List<Productos> findByPrecioBetween(@Param("min") BigDecimal min, @Param("max") BigDecimal max);

    @Query("SELECT p, COUNT(oi.id) as ventas FROM Productos p " +
    "LEFT JOIN OrdenItem oi ON p.id = oi.producto.id " +
    "GROUP BY p.id ORDER BY ventas DESC LIMIT :cantidad")
List<Object[]> findTopNByOrderByVentasDesc(int cantidad);
}

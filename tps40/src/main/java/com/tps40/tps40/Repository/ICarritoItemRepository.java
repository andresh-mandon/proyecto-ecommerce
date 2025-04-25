package com.tps40.tps40.Repository;
import com.tps40.tps40.Entities.Carrito;
import com.tps40.tps40.Entities.CarritoItem;
import com.tps40.tps40.Entities.Productos;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ICarritoItemRepository extends JpaRepository<CarritoItem, Long> {
    Optional<CarritoItem> findByCarritoAndProducto(Carrito carrito, Productos producto);
    List<CarritoItem> findByCarrito(Carrito carrito);
    void deleteByCarrito(Carrito carrito);
    @Query("SELECT ci FROM CarritoItem ci JOIN FETCH ci.producto WHERE ci.carrito.id = :carritoId")
    List<CarritoItem> findByCarritoIdWithProducto(@Param("carritoId") Long carritoId);

@Modifying
@Query("DELETE FROM CarritoItem c WHERE c.carrito.id = :carritoId")
void deleteAllByCarritoId(Long carritoId);
List<CarritoItem> findByCarritoId(Long carritoId);

@Modifying // Fundamental: Indica que la query modifica el estado de la BD (no es un SELECT)
@Query("DELETE FROM CarritoItem ci WHERE ci.carrito.id = :carritoId") // JPQL para borrar por el ID del carrito asociado
int deleteByCarritoId(@Param("carritoId") Long carritoId); 
}

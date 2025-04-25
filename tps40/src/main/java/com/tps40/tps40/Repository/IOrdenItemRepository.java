package com.tps40.tps40.Repository;
import com.tps40.tps40.Entities.CarritoItem;
import com.tps40.tps40.Entities.Orden;
import com.tps40.tps40.Entities.OrdenItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface IOrdenItemRepository extends JpaRepository<OrdenItem, Long> {
    List<OrdenItem> findByOrden(Orden orden);
    @Query("SELECT ci FROM CarritoItem ci JOIN FETCH ci.producto WHERE ci.carrito.id = :carritoId")
List<CarritoItem> findByCarritoIdWithProducto(@Param("carritoId") Long carritoId);
}

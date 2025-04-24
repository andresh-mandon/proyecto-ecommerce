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
    @Modifying
    @Query("DELETE FROM CarritoItem ci WHERE ci.carrito = :carrito")
    void deleteAllByCarrito(@Param("carrito") Carrito carrito);
}

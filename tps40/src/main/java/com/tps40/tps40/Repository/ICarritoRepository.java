package com.tps40.tps40.Repository;
import com.tps40.tps40.Entities.Carrito;
import com.tps40.tps40.Entities.Usuarios;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ICarritoRepository extends JpaRepository<Carrito, Long> {
    // Busca el carrito activo del usuario (sin orden asociada)
    @Query("SELECT c FROM Carrito c WHERE c.usuario = :usuario AND NOT EXISTS (SELECT o FROM Orden o WHERE o.carrito = c)")
    Optional<Carrito> findCarritoActivoByUsuario(@Param("usuario") Usuarios usuario);

  
}

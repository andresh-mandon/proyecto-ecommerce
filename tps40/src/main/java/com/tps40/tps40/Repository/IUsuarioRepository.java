package com.tps40.tps40.Repository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import com.tps40.tps40.Entities.Usuarios;
import java.util.Optional;
import java.util.List;

@Repository
public interface IUsuarioRepository extends JpaRepository<Usuarios, Long> {
    Optional<Usuarios> findByNombreUsuario(String nombreUsuario);
    Optional<Usuarios> findByEmail(String email);
    boolean existsByEmail(String email);
    boolean existsByNombreUsuario(String nombreUsuario);

     @Query("SELECT u, COUNT(o.id) as compras FROM Usuarios u " +
           "LEFT JOIN Orden o ON u.id = o.usuario.id " +
           "GROUP BY u.id ORDER BY compras DESC LIMIT :cantidad")
    List<Object[]> findTopNByOrderByComprasDesc(int cantidad);
}

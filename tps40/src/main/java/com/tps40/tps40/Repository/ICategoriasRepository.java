package com.tps40.tps40.Repository;
import com.tps40.tps40.Entities.Categorias;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ICategoriasRepository extends JpaRepository<Categorias, Long> {
    // Consulta personalizada para buscar por nombre (opcional)
    List<Categorias> findByNombreContainingIgnoreCase(String nombre);
    Optional<Categorias> findByNombre(String nombre);
 
}

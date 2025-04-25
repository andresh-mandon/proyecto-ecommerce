package com.tps40.tps40.Service;
import com.tps40.tps40.Repository.ICategoriasRepository;
import com.tps40.tps40.Entities.Categorias;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class CategoriasService {
    @Autowired
    private ICategoriasRepository categoriaRepository;

    // Crear categoría (solo ADMIN)
    public Categorias crearCategoria(Categorias categoria) {
        return categoriaRepository.save(categoria);
    }

    // Actualizar categoría (solo ADMIN)
    public Categorias actualizarCategoria(Long id, Categorias categoriaActualizada) {
        Categorias categoria = categoriaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Categoría no encontrada"));
        categoria.setNombre(categoriaActualizada.getNombre());
        categoria.setDescripcion(categoriaActualizada.getDescripcion());
        return categoriaRepository.save(categoria);
    }

    // Eliminar categoría (solo ADMIN)
    public void eliminarCategoria(Long id) {
        categoriaRepository.deleteById(id);
    }

    // Listar todas las categorías (público)
    public List<Categorias> listarCategorias() {
        return categoriaRepository.findAll(); // Debe devolver todas las categorías
    }
    

    // Buscar por nombre (público)
    public List<Categorias> buscarPorNombre(String nombre) {
        return categoriaRepository.findByNombreContainingIgnoreCase(nombre);
    }
    public Categorias obtenerCategoriaPorId(Long id) {
        Optional<Categorias> categoria = categoriaRepository.findById(id);
        if (categoria.isPresent()) {
            return categoria.get();
        } else {
            throw new RuntimeException("No se encontró la categoría con ID: " + id);
        }
    }
}

package com.tps40.tps40.Service;
import com.tps40.tps40.Entities.Productos;
import com.tps40.tps40.Repository.ICategoriasRepository;
import com.tps40.tps40.Repository.IProductosRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class ProductosService {
    @Autowired
    private IProductosRepository productoRepository;

    @Autowired
    private ICategoriasRepository categoriaRepository;

    // Crear producto (solo ADMIN)
    @Transactional
    public Productos crearProducto(Productos producto) {
        return productoRepository.save(producto);
    }

    // Actualizar producto (solo ADMIN)
    @Transactional
    public Productos actualizarProducto(Long id, Productos productoActualizado) {
        Productos producto = productoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado"));
        
        // Actualizar solo los campos permitidos
        producto.setNombre(productoActualizado.getNombre());
        producto.setPrecio(productoActualizado.getPrecio());
        producto.setStock(productoActualizado.getStock());
        producto.setDescripcion(productoActualizado.getDescripcion());
        
        // Actualizar categoría si se proporciona
        if (productoActualizado.getCategoria() != null) {
            producto.setCategoria(productoActualizado.getCategoria());
        }
        
        // Mantener la imagen actual si no se proporciona nueva
        if (productoActualizado.getImagen() != null) {
            producto.setImagen(productoActualizado.getImagen());
        }
        
        return productoRepository.save(producto);
    }

    @Transactional
    public void eliminarProducto(Long id) {
        if (!productoRepository.existsById(id)) {
            throw new RuntimeException("Producto no encontrado");
        }
        productoRepository.deleteById(id);
    }

    // Listar todos los productos
    public List<Productos> listarProductos() {
        return productoRepository.findAll();
    }

    // Buscar por nombre
    public List<Productos> buscarPorNombre(String nombre) {
        return productoRepository.findByNombreContainingIgnoreCase(nombre);
    }

    // Buscar por categoría
    public List<Productos> buscarPorCategorias(List<Long> categoriaIds) {
        return productoRepository.findByCategoriaIdIn(categoriaIds);
    }

    // Buscar por rango de precios
    public List<Productos> buscarPorRangoPrecios(BigDecimal min, BigDecimal max) {
        return productoRepository.findByPrecioBetween(min, max);
    }

    // Método necesario para el CarritoService
    public Productos obtenerProductoPorId(Long id) {
        return productoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado"));
    }

    // Método para actualizar stock
    @Transactional
    public void actualizarStock(Long productoId, Integer cantidadVendida) {
        Productos producto = obtenerProductoPorId(productoId);
        if (producto.getStock() < cantidadVendida) {
            throw new RuntimeException("Stock insuficiente");
        }
        producto.setStock(producto.getStock() - cantidadVendida);
        productoRepository.save(producto);
    }
}

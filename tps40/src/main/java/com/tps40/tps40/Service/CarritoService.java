package com.tps40.tps40.Service;
import com.tps40.tps40.Entities.Carrito;
import com.tps40.tps40.Entities.CarritoItem;
import com.tps40.tps40.Entities.Productos;
import com.tps40.tps40.Entities.Usuarios;
import com.tps40.tps40.Repository.ICarritoItemRepository;
import com.tps40.tps40.Repository.ICarritoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class CarritoService {
    @Autowired
    private ICarritoRepository carritoRepository;

    @Autowired
    private ICarritoItemRepository carritoItemRepository;

    @Autowired
    private ProductosService productoService;

    @Autowired
    private UsuarioService usuarioService;

    @Transactional
    public Carrito crearCarritoParaUsuario(Long usuarioId) {
        Usuarios usuario = usuarioService.obtenerUsuarioPorId(usuarioId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        Carrito carrito = new Carrito();
        carrito.setUsuario(usuario);
        return carritoRepository.save(carrito);
    }

    // Obtener o crear carrito activo
    @Transactional(readOnly = true)
    public Carrito obtenerCarritoActivo(Long usuarioId) {
        return carritoRepository.findCarritoActivoByUsuarioId(usuarioId)
                .orElseGet(() -> {
                    Usuarios usuario = usuarioService.obtenerUsuarioPorId(usuarioId)
                            .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

                    Carrito nuevoCarrito = new Carrito();
                    nuevoCarrito.setUsuario(usuario);
                    nuevoCarrito.setFechaCreacion(LocalDateTime.now());
                    nuevoCarrito.setTotal(BigDecimal.ZERO);
                    return carritoRepository.save(nuevoCarrito);
                });
    }

    // Agregar producto al carrito
    @Transactional
    public void agregarProducto(Long usuarioId, Long productoId, Integer cantidad) {
        Carrito carrito = obtenerCarritoActivo(usuarioId); // Usa el método corregido
        Productos producto = productoService.obtenerProductoPorId(productoId);

        // Busca si el producto ya está en el carrito
        Optional<CarritoItem> itemExistente = carritoItemRepository.findByCarritoAndProducto(carrito, producto);

        if (itemExistente.isPresent()) {
            // Actualiza cantidad y subtotal
            CarritoItem item = itemExistente.get();
            item.setCantidad(item.getCantidad() + cantidad);
            item.setSubtotal(producto.getPrecio().multiply(BigDecimal.valueOf(item.getCantidad())));
        } else {
            // Crea nuevo ítem
            CarritoItem nuevoItem = new CarritoItem();
            nuevoItem.setCarrito(carrito);
            nuevoItem.setProducto(producto);
            nuevoItem.setCantidad(cantidad);
            nuevoItem.setSubtotal(producto.getPrecio().multiply(BigDecimal.valueOf(cantidad)));
            carritoItemRepository.save(nuevoItem);
        }

        actualizarTotalCarrito(carrito); // Actualiza el total del carrito
    }

    // Actualizar cantidad de un item
    @Transactional
    public Carrito actualizarCantidadItem(Long itemId, Integer cantidad) {
        CarritoItem item = carritoItemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Item no encontrado"));

        if (cantidad <= 0) {
            carritoItemRepository.delete(item);
        } else {
            // Verificar stock
            if (item.getProducto().getStock() < cantidad) {
                throw new RuntimeException("Stock insuficiente");
            }
            item.setCantidad(cantidad);
            item.setSubtotal(item.getProducto().getPrecio().multiply(BigDecimal.valueOf(cantidad)));
            carritoItemRepository.save(item);
        }

        Carrito carrito = item.getCarrito();
        actualizarTotalCarrito(carrito);
        return carrito;
    }

    // Eliminar item del carrito
    @Transactional
    public void eliminarItem(Long itemId, Long usuarioId) {
        // 1. Obtener el carrito activo del usuario
        Carrito carrito = obtenerCarritoActivo(usuarioId);

        // 2. Buscar el item en el carrito
        CarritoItem item = carritoItemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Ítem no encontrado"));

        // 3. Verificar que el item pertenece al carrito del usuario
        if (!item.getCarrito().getId().equals(carrito.getId())) {
            throw new RuntimeException("El ítem no pertenece a tu carrito");
        }

        // 4. Eliminar el item
        carritoItemRepository.delete(item);

        // 5. Actualizar el total del carrito
        actualizarTotalCarrito(carrito);

        // 6. Eliminar el carrito si queda vacío (opcional)
        if (carrito.getItems().isEmpty()) {
            carritoRepository.delete(carrito);
        }
    }

    // Vaciar carrito completamente
    @Transactional
    public void vaciarCarrito(Long carritoId) {
        Carrito carrito = carritoRepository.findById(carritoId)
                .orElseThrow(() -> new RuntimeException("Carrito no encontrado"));

        // Eliminar todos los items asociados
        carritoItemRepository.deleteAllByCarrito(carrito); // ¡Nuevo método necesario!

        // Resetear total y fecha
        carrito.setTotal(BigDecimal.ZERO);
        carrito.setFechaCreacion(LocalDateTime.now());
        carritoRepository.save(carrito);
    }

    // Obtener items del carrito
    public List<CarritoItem> obtenerItemsDeCarrito(Long carritoId) {
        Carrito carrito = carritoRepository.findById(carritoId)
                .orElseThrow(() -> new RuntimeException("Carrito no encontrado"));
        return carritoItemRepository.findByCarrito(carrito);
    }

    // Método auxiliar para actualizar total
    private void actualizarTotalCarrito(Carrito carrito) {
        BigDecimal total = carrito.getItems().stream()
                .map(CarritoItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        carrito.setTotal(total);
        carritoRepository.save(carrito);
    }
}

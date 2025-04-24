package com.tps40.tps40.Service;
import com.tps40.tps40.Entities.Carrito;
import com.tps40.tps40.Entities.CarritoItem;
import com.tps40.tps40.Entities.Orden;
import com.tps40.tps40.Entities.OrdenItem;
import com.tps40.tps40.Repository.IOrdenItemRepository;
import com.tps40.tps40.Repository.IOrdenRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class OrdenService {
    @Autowired
    private IOrdenRepository ordenRepository;

    @Autowired
    private IOrdenItemRepository ordenItemRepository;

    @Autowired
    private CarritoService carritoService;

    @Autowired
    private ProductosService productoService;

    @Autowired
    private UsuarioService usuarioService;

    // Crear orden desde carrito
    @Transactional
    public Orden crearOrdenDesdeCarrito(Long carritoId, String direccionEnvio) {
        Carrito carrito = carritoService.obtenerCarritoActivo(carritoId);

        // Validar que el carrito no esté vacío
        if (carrito.getItems() == null || carrito.getItems().isEmpty()) {
            throw new IllegalStateException("No se puede crear una orden con un carrito vacío");
        }

        // Configurar la orden
        Orden orden = new Orden();
        orden.setUsuario(carrito.getUsuario());
        orden.setFechaCreacion(LocalDateTime.now());
        orden.setTotal(carrito.getTotal());
        orden.setDireccionEnvio(direccionEnvio);
        orden.setEstado(Orden.EstadoOrden.PENDIENTE);
        orden.setCarrito(carrito);  // Vincular con el carrito origen

        // Guardar la orden primero para generar ID
        orden = ordenRepository.save(orden);

        // Crear y vincular los items de la orden
        for (CarritoItem item : carrito.getItems()) {
            OrdenItem ordenItem = new OrdenItem();
            ordenItem.setOrden(orden);
            ordenItem.setProducto(item.getProducto());
            ordenItem.setCantidad(item.getCantidad());
            ordenItem.setPrecioUnitario(item.getProducto().getPrecio());
            ordenItem.setSubtotal(item.getSubtotal());

            // Actualizar stock del producto
            productoService.actualizarStock(
                    item.getProducto().getId(),
                    -item.getCantidad()  // Restar del stock
            );

            orden.getItems().add(ordenItem);
        }

        // Vaciar el carrito
        carritoService.vaciarCarrito(carritoId);

        return ordenRepository.save(orden);  // Guardar cambios finales
    }

    // Listar órdenes por usuario
    public List<Orden> listarOrdenesPorUsuario(Long usuarioId) {
        return ordenRepository.findByUsuario(
                usuarioService.obtenerUsuarioPorId(usuarioId)
                        .orElseThrow(() -> new RuntimeException("Usuario no encontrado"))
        );
    }

    // Actualizar estado (solo ADMIN)
    @Transactional
    public Orden actualizarEstadoOrden(Long ordenId, Orden.EstadoOrden nuevoEstado) {
        Orden orden = ordenRepository.findById(ordenId)
                .orElseThrow(() -> new RuntimeException("Orden no encontrada"));
        orden.setEstado(nuevoEstado);
        return ordenRepository.save(orden);
    }

    // Obtener detalles de orden
    public Orden obtenerOrdenConItems(Long ordenId) {
        Orden orden = ordenRepository.findById(ordenId)
                .orElseThrow(() -> new RuntimeException("Orden no encontrada"));
        orden.setItems(ordenItemRepository.findByOrden(orden));
        return orden;
    }
}

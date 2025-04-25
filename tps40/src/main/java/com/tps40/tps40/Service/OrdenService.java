package com.tps40.tps40.Service;

import com.tps40.tps40.Entities.*;
import com.tps40.tps40.Repository.ICarritoItemRepository;
import com.tps40.tps40.Repository.IOrdenItemRepository;
import com.tps40.tps40.Repository.IOrdenRepository;
import com.tps40.tps40.Repository.IProductosRepository; // Asegúrate de tener este repo
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional; // Usa este

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class OrdenService {

    private static final Logger log = LoggerFactory.getLogger(OrdenService.class);

    @Autowired
    private IOrdenRepository ordenRepository;

    @Autowired
    private IOrdenItemRepository ordenItemRepository;

    @Autowired
    private ICarritoItemRepository carritoItemRepository;
    
    @Autowired
    private IProductosRepository productoRepository; // Necesario para actualizar stock directamente si es el caso

    @Autowired
    private ProductosService productoService; // O usar este si ya tiene la lógica de stock

    @Autowired
    private CarritoService carritoService;
    
    @Autowired
    private UsuarioService usuarioService; // Para obtener el usuario

    public List<Orden> obtenerTodasLasOrdenes() {
        return ordenRepository.findAll();
    }

    public Orden obtenerOrdenPorId(Long id) {
        return ordenRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Orden no encontrada con ID: " + id));
    }

    public List<Orden> obtenerOrdenesPorEstado(Orden.EstadoOrden estado) {
        return ordenRepository.findByEstado(estado);
    }

    public List<Orden> obtenerOrdenesPorUsuario(Usuarios usuario) {
        return ordenRepository.findByUsuarioOrderByFechaCreacionDesc(usuario);
    }

    @Transactional
    public Orden actualizarEstadoOrden(Long ordenId, Orden.EstadoOrden nuevoEstado) {
        Orden orden = obtenerOrdenPorId(ordenId);
        orden.setEstado(nuevoEstado);
        return ordenRepository.save(orden);
    }

    @Transactional
    public void eliminarOrden(Long ordenId) {
        // Considera la lógica de negocio: ¿debería restaurarse el stock?
        // ¿Qué pasa con los OrdenItems? Podrían necesitar borrado en cascada.
        Orden orden = obtenerOrdenPorId(ordenId);
        ordenRepository.delete(orden);
        log.info("Orden eliminada: {}", ordenId);
    }

    
    @Transactional
    public Orden crearOrdenDesdeCarrito(Long usuarioId, String direccionEnvio) {
        log.info("Iniciando creación de orden para usuario ID: {}", usuarioId);

        Usuarios usuario = usuarioService.obtenerUsuarioPorId(usuarioId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado: " + usuarioId));
        Carrito carrito = carritoService.obtenerCarritoActivo(usuarioId);
        log.debug("Carrito activo ID: {} obtenido para usuario {}", carrito.getId(), usuarioId);

        List<CarritoItem> carritoItems = carritoService.obtenerItemsDeCarrito(carrito.getId());
        if (carritoItems.isEmpty()) {
            log.warn("Intento de crear orden con carrito vacío para usuario ID: {}", usuarioId);
            throw new RuntimeException("Tu carrito está vacío.");
        }
        log.debug("Items en carrito: {}", carritoItems.size());

        BigDecimal totalCalculado = BigDecimal.ZERO;
        for (CarritoItem item : carritoItems) {
            Productos producto = item.getProducto();
            Optional<Productos> productoActualOpt = productoRepository.findById(producto.getId());
            if (productoActualOpt.isEmpty()) {
                 throw new RuntimeException("Producto no encontrado en la base de datos: ID " + producto.getId());
            }
            Productos productoActual = productoActualOpt.get();

            log.debug("Verificando stock para producto ID {}: requerido {}, disponible {}",
                      productoActual.getId(), item.getCantidad(), productoActual.getStock());
            if (productoActual.getStock() < item.getCantidad()) {
                log.error("Stock insuficiente para producto ID {}: {} requerido, {} disponible",
                          productoActual.getId(), item.getCantidad(), productoActual.getStock());
                throw new RuntimeException("Stock insuficiente para el producto: " + productoActual.getNombre() + ". Disponible: " + productoActual.getStock());
            }
            BigDecimal subtotalItem = productoActual.getPrecio().multiply(BigDecimal.valueOf(item.getCantidad()));
            totalCalculado = totalCalculado.add(subtotalItem);
        }
        log.debug("Verificación de stock completada. Total calculado: {}", totalCalculado);
        if (totalCalculado.compareTo(carrito.getTotal()) != 0) {
             log.warn("Discrepancia en el total del carrito. Calculado: {}, Carrito: {}. Usando el calculado.", totalCalculado, carrito.getTotal());
        }

         // 4. Crear la Orden
         Orden orden = new Orden();
         orden.setUsuario(usuario);
         orden.setCarrito(carrito); // Asignar carrito
         orden.setFechaCreacion(LocalDateTime.now());
         orden.setTotal(totalCalculado); // Usar el total verificado
         orden.setEstado(Orden.EstadoOrden.PENDIENTE);
         orden.setDireccionEnvio(direccionEnvio != null ? direccionEnvio : usuario.getDireccion());
 
         // 5. Guardar la Orden
         Orden ordenGuardada = ordenRepository.save(orden);
         log.info("Orden ID: {} creada exitosamente en estado PENDIENTE", ordenGuardada.getId());
 
         // 6. Crear OrdenItems y DESCONTAR stock
         for (CarritoItem item : carritoItems) {
             OrdenItem ordenItem = new OrdenItem();
             ordenItem.setOrden(ordenGuardada);
             ordenItem.setProducto(item.getProducto());
             ordenItem.setCantidad(item.getCantidad());
             ordenItem.setPrecioUnitario(item.getProducto().getPrecio());
             ordenItem.setSubtotal(item.getSubtotal()); // Usar subtotal del item del carrito
 
             ordenItemRepository.save(ordenItem);
             log.debug("OrdenItem guardado para Producto ID: {} en Orden ID: {}", item.getProducto().getId(), ordenGuardada.getId());
 
             // --- INICIO: CORRECCIÓN AQUÍ ---
             try {
                 // Pasar la cantidad del item POSITIVA
                 Integer cantidadVendida = item.getCantidad();
                 productoService.actualizarStock(item.getProducto().getId(), cantidadVendida); // <--- Sin el signo negativo
 
                 // El log ahora es consistente con la acción (restar cantidadVendida)
                 log.info("Stock actualizado para Producto ID: {}. Cantidad reducida en: {}", item.getProducto().getId(), cantidadVendida);
 
             } catch (RuntimeException e) { // Capturar RuntimeException específicamente si actualizarStock la lanza
                 log.error("Error CRÍTICO al actualizar stock para producto ID: {}. Revirtiendo transacción. Error: {}",
                           item.getProducto().getId(), e.getMessage(), e);
                 // Relanzar la excepción para asegurar el rollback de la transacción @Transactional
                 throw new RuntimeException("Error al actualizar el stock para " + item.getProducto().getNombre() + ". La orden no pudo crearse completamente. Causa: " + e.getMessage(), e);
             } catch (Exception e){ // Capturar otras excepciones inesperadas
                 log.error("Error inesperado al actualizar stock para producto ID: {}.", item.getProducto().getId(), e);
                 throw new RuntimeException("Error inesperado durante actualización de stock. Orden no creada.", e);
             }
             // --- FIN: CORRECCIÓN AQUÍ ---
         }
 
         // 7. Vaciar el carrito original (usar la implementación de CarritoService)
         carritoService.vaciarCarrito(carrito.getId());
         log.info("Carrito ID: {} vaciado después de crear la Orden ID: {}", carrito.getId(), ordenGuardada.getId());
 
         return ordenGuardada;
     }

    // Método para vaciar el carrito (elimina los items)
    private void vaciarCarrito(Long carritoId) {
        log.debug("Intentando vaciar carrito ID: {}", carritoId);
        List<CarritoItem> items = carritoItemRepository.findByCarritoId(carritoId);
        if (!items.isEmpty()) {
             carritoItemRepository.deleteAll(items); // O usar deleteAllByCarritoId si lo tienes
             log.debug("Eliminados {} items del carrito ID: {}", items.size(), carritoId);
        } else {
             log.debug("El carrito ID: {} ya estaba vacío o no se encontraron items.", carritoId);
        }

        // Actualizar el total del carrito a 0
        Carrito carrito = carritoService.obtenerCarritoPorId(carritoId); // Necesitas este método en CarritoService
        if (carrito != null) {
            carrito.setTotal(BigDecimal.ZERO);
            carritoService.actualizarCarrito(carrito); // Necesitas este método en CarritoService
             log.debug("Total del carrito ID: {} actualizado a 0.", carritoId);
        } else {
            log.warn("No se pudo encontrar el carrito ID: {} para actualizar su total a 0.", carritoId);
        }
    }
    
     // Necesitarás añadir estos métodos en CarritoService y su Repository si no existen:
     // Carrito obtenerCarritoPorId(Long carritoId);
     // void actualizarCarrito(Carrito carrito);
     // List<CarritoItem> findByCarritoId(Long carritoId); // En ICarritoItemRepository
     // void deleteAllByCarritoId(Long carritoId); // Opcional, alternativa a find+deleteAll

    // La clase interna OrdenItemData no parece usarse, se puede eliminar.
}
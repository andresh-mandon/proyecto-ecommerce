package com.tps40.tps40.Controller;
import com.tps40.tps40.Entities.Carrito;
import com.tps40.tps40.Entities.CarritoItem;
import com.tps40.tps40.Entities.Productos;
import com.tps40.tps40.Entities.Usuarios;
import com.tps40.tps40.Service.CarritoService;
import com.tps40.tps40.Service.ProductosService;
import com.tps40.tps40.Service.UsuarioService;

import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/carrito")
public class CarritoController {
    @Autowired
    private CarritoService carritoService;
    @Autowired
    private UsuarioService usuarioService;
    @Autowired
    private ProductosService productoService;

    @Autowired
    public CarritoController(CarritoService carritoService,
                             UsuarioService usuarioService,
                             ProductosService productoService) {
        this.carritoService = carritoService;
        this.usuarioService = usuarioService;
        this.productoService = productoService;
    }

    // Ver carrito con manejo completo de errores
    @GetMapping
public String verCarrito(@AuthenticationPrincipal UserDetails userDetails, 
                        Model model,
                        HttpServletResponse response) {
    response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
    response.setHeader("Pragma", "no-cache");
    response.setHeader("Expires", "0");
    
    try {
        Usuarios usuario = usuarioService.obtenerUsuarioPorEmail(userDetails.getUsername());
        Carrito carrito = carritoService.obtenerCarritoActivo(usuario.getId());
        
        // Cargar items con productos
        List<CarritoItem> items = carritoService.obtenerItemsDeCarrito(carrito.getId());
        
        // Debug detallado
        System.out.println("=== DEBUG DEL CARRITO ===");
        System.out.println("Carrito ID: " + carrito.getId());
        System.out.println("Total items: " + items.size());
        
        for (CarritoItem item : items) {
            System.out.println("Item ID: " + item.getId());
            System.out.println(" - Producto ID: " + item.getProducto().getId());
            System.out.println(" - Nombre: " + item.getProducto().getNombre());
            System.out.println(" - Precio: " + item.getProducto().getPrecio());
            System.out.println(" - Cantidad: " + item.getCantidad());
            System.out.println(" - Subtotal: " + item.getSubtotal());
        }
        
        carrito.setItems(items);
        model.addAttribute("carrito", carrito);
        return "carrito";
    } catch (Exception e) {
        e.printStackTrace();
        model.addAttribute("error", "Error al cargar el carrito: " + e.getMessage());
        return "error";
    }
}
    @PostMapping("/agregar")
public String agregarProducto(
    @AuthenticationPrincipal UserDetails userDetails,
    @RequestParam Long productoId,
    @RequestParam(required = false, defaultValue = "1") Integer cantidad,
    RedirectAttributes redirectAttributes) {

    try {
        Usuarios usuario = usuarioService.obtenerUsuarioPorEmail(userDetails.getUsername());
        Productos producto = productoService.obtenerProductoPorId(productoId);
        
        carritoService.agregarProducto(usuario.getId(), producto.getId(), cantidad);
        
        redirectAttributes.addFlashAttribute("success", "Producto agregado al carrito");
    } catch (Exception e) {
        redirectAttributes.addFlashAttribute("error", "Error al agregar producto: " + e.getMessage());
    }
    
    return "redirect:/productos/productos_usuarios"; // Redirige de vuelta a la página de productos
}

    // Actualizar cantidad de un item
    @PostMapping("/item/{itemId}/actualizar")
    @ResponseBody
    public ResponseEntity<?> actualizarCantidadItem(@PathVariable Long itemId,
                                         @RequestParam Integer cantidad) {
        try {
            carritoService.actualizarCantidadItem(itemId, cantidad);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // Eliminar item del carrito
    @PostMapping("/item/{itemId}/eliminar")
    @ResponseBody
    public ResponseEntity<?> eliminarItem(@PathVariable Long itemId) {
        try {
            carritoService.eliminarItem(itemId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // Vaciar carrito completo
    @PostMapping("/vaciar")
    @ResponseBody
    public ResponseEntity<?> vaciarCarrito(@AuthenticationPrincipal UserDetails userDetails) {
        try {
            Usuarios usuario = usuarioService.obtenerUsuarioPorNombreUsuario(userDetails.getUsername());
            Carrito carrito = carritoService.obtenerCarritoActivo(usuario.getId());

            carritoService.vaciarCarrito(carrito.getId());
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
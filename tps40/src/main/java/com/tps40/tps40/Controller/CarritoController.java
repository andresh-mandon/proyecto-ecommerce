package com.tps40.tps40.Controller;
import com.tps40.tps40.Entities.Carrito;
import com.tps40.tps40.Entities.Productos;
import com.tps40.tps40.Entities.Usuarios;
import com.tps40.tps40.Service.CarritoService;
import com.tps40.tps40.Service.ProductosService;
import com.tps40.tps40.Service.UsuarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.ArrayList;

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
                             @RequestParam(required = false) String error) {
        try {
            Usuarios usuario = usuarioService.obtenerUsuarioPorEmail(userDetails.getUsername());
            Carrito carrito = carritoService.obtenerCarritoActivo(usuario.getId());
            if (carrito == null) {
                carrito = new Carrito(); // Carrito vacío
                carrito.setItems(new ArrayList<>());
                carrito.setTotal(BigDecimal.ZERO);
            }

            model.addAttribute("carrito", carrito);
            model.addAttribute("items", carritoService.obtenerItemsDeCarrito(carrito.getId()));

            if (error != null) {
                model.addAttribute("error", error);
            }

            return "carrito";

        } catch (Exception e) {
            model.addAttribute("error", "Error al cargar el carrito: " + e.getMessage());
            return "error";
        }
    }

    // Agregar producto al carrito
    @PostMapping("/agregar")
    public String agregarProducto(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam Long productoId, // Asegúrate de recibir el ID correctamente
            @RequestParam(defaultValue = "1") Integer cantidad,
            RedirectAttributes redirectAttributes) {

        try {
            Usuarios usuario = usuarioService.obtenerUsuarioPorEmail(userDetails.getUsername());
            carritoService.agregarProducto(usuario.getId(), productoId, cantidad);
            redirectAttributes.addFlashAttribute("success", "Producto agregado al carrito");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error: " + e.getMessage());
        }
        return "redirect:/productos/productos_usuarios"; // Redirige a la vista correcta
    }

    // Actualizar cantidad de un item
    @PostMapping("/item/{itemId}/actualizar")
    public String actualizarCantidadItem(@PathVariable Long itemId,
                                         @RequestParam Integer cantidad,
                                         RedirectAttributes redirectAttributes) {
        try {
            carritoService.actualizarCantidadItem(itemId, cantidad);
            return "redirect:/carrito";
        } catch (Exception e) {
            redirectAttributes.addAttribute("error", e.getMessage());
            return "redirect:/carrito";
        }
    }

    // Eliminar item del carrito
    @PostMapping("/item/{itemId}/eliminar")
    public String eliminarItem(
            @PathVariable Long itemId,
            @AuthenticationPrincipal UserDetails userDetails,
            RedirectAttributes redirectAttributes) {

        try {
            Usuarios usuario = usuarioService.obtenerUsuarioPorEmail(userDetails.getUsername());
            carritoService.eliminarItem(itemId, usuario.getId());
            redirectAttributes.addFlashAttribute("success", "Producto eliminado del carrito");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/carrito";
    }

    // Vaciar carrito completo
    @PostMapping("/vaciar")
    public String vaciarCarrito(@AuthenticationPrincipal UserDetails userDetails,
                                RedirectAttributes redirectAttributes) {
        try {
            Usuarios usuario = usuarioService.obtenerUsuarioPorNombreUsuario(userDetails.getUsername());
            Carrito carrito = carritoService.obtenerCarritoActivo(usuario.getId());

            carritoService.vaciarCarrito(carrito.getId());
            return "redirect:/carrito";

        } catch (Exception e) {
            redirectAttributes.addAttribute("error", e.getMessage());
            return "redirect:/carrito";
        }
    }

    // Finalizar compra (redirige a órdenes)
    @PostMapping("/finalizar")
    public String finalizarCompra(@AuthenticationPrincipal UserDetails userDetails,
                                  RedirectAttributes redirectAttributes) {
        try {
            return "redirect:/crear";
        } catch (Exception e) {
            redirectAttributes.addAttribute("error", e.getMessage());
            return "redirect:/carrito";
        }
    }

}

package com.tps40.tps40.Controller;

import org.springframework.util.StringUtils;
import com.tps40.tps40.Entities.Categorias;
import com.tps40.tps40.Entities.Productos;
import com.tps40.tps40.Service.CategoriasService;
import com.tps40.tps40.Service.ProductosService;
// Asegúrate de importar UsuarioService si lo necesitas en otros métodos
// import com.tps40.tps40.Service.UsuarioService; 

import jakarta.servlet.http.HttpSession;
import jakarta.transaction.Transactional; // Correcto

import java.io.IOException;
import java.io.InputStream; // Para Files.copy
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path; // Importar Path
import java.nio.file.Paths; // Importar Paths
import java.nio.file.StandardCopyOption; // Para reemplazar si existe
import java.util.List;
import java.util.UUID; // Para nombres de archivo únicos más robustos

import org.slf4j.Logger; // Importar Logger
import org.slf4j.LoggerFactory; // Importar LoggerFactory
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value; // Para leer desde properties
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;


@Controller
public class ProductosController {

    private static final Logger log = LoggerFactory.getLogger(ProductosController.class); // Logger

    private final ProductosService productoService;
    private final CategoriasService categoriaService;

    // --- Configuración de Carga de Archivos (MEJORADO) ---
    // Puedes definir esto en application.properties: upload.path=./uploads/
    @Value("${file.upload-dir}")
private String uploadPath;

    // Ya no necesitas UPLOAD_DIR estático

    // Inyección por constructor (recomendado)
    @Autowired // Opcional si solo tienes un constructor
    public ProductosController(ProductosService productoService, CategoriasService categoriaService) {
        this.productoService = productoService;
        this.categoriaService = categoriaService;
    }

    @GetMapping("/productos/productos_usuarios")
    public String mostrarProductosUsuarios(
            @RequestParam(name = "categoriaIds", required = false) List<Long> categoriaIds,
            Model model) {

        List<Categorias> categorias = categoriaService.listarCategorias();
        model.addAttribute("categorias", categorias);

        List<Productos> productos;
        if (categoriaIds != null && !categoriaIds.isEmpty()) {
            productos = productoService.buscarPorCategorias(categoriaIds);
        } else {
            productos = productoService.listarProductos();
        }

        model.addAttribute("productos", productos);
        model.addAttribute("categoriaIds", categoriaIds); // Mantener selección

        return "productos_usuarios";
    }


    // --- Rutas de Administración ---
    
    @GetMapping("/logout")
    public String cerrarSesion(HttpSession session) {
        session.invalidate();
        return "redirect:/usuarios/login?logout";
    }
    @GetMapping("/admin/productos")
    @PreAuthorize("hasRole('ADMIN')") // Asegurar también esta ruta si es necesario
    public String listarProductos(Model model) {
        List<Productos> productos = productoService.listarProductos();
        model.addAttribute("productos", productos);
        return "admin/productos_admin";
    }

    @GetMapping("/admin/productos/nuevo")
@PreAuthorize("hasRole('ADMIN')")
public String mostrarFormularioCreacion(Model model) {
    List<Categorias> listaCategorias = categoriaService.listarCategorias();
    System.out.println("Número de categorías encontradas: " + listaCategorias.size());
    listaCategorias.forEach(cat -> System.out.println(cat.getNombre()));
    
    model.addAttribute("producto", new Productos());
    model.addAttribute("categorias", listaCategorias);
    return "admin/add_productos";
}

    @Transactional // Buena práctica mantenerla aquí si hay varias operaciones DB
    @PostMapping("/admin/productos/agregar")
public String guardarProducto(
        @ModelAttribute("producto") Productos producto,  // Recibe el objeto bindeado
        @RequestParam("imagenFile") MultipartFile imagenFile,
        RedirectAttributes redirectAttributes) {
    
    try {
        // Validar categoría
        if (producto.getCategoria() == null || producto.getCategoria().getId() == null) {
            redirectAttributes.addFlashAttribute("error", "Debe seleccionar una categoría");
            return "redirect:/admin/productos/nuevo";
        }
        
        // Guardar imagen
        if (imagenFile != null && !imagenFile.isEmpty()) {
            String nombreImagen = guardarImagen(imagenFile);
            producto.setImagen(nombreImagen);
        } else {
            producto.setImagen("default-product.png");
        }
        
        productoService.crearProducto(producto);
        redirectAttributes.addFlashAttribute("success", "Producto creado exitosamente");
        return "redirect:/admin/productos";
        
    } catch (Exception e) {
        redirectAttributes.addFlashAttribute("error", "Error: " + e.getMessage());
        return "redirect:/admin/productos/nuevo";
    }
}

private String guardarImagen(MultipartFile imagen) throws IOException {
    Path directorioBase = Paths.get(uploadPath).toAbsolutePath().normalize();

    
    // Validación reforzada
    if (!Files.exists(directorioBase)) {
        throw new IOException("Directorio base no existe: " + directorioBase);
    }

    String nombreArchivo = String.format("%s_%s", 
        UUID.randomUUID(), 
        StringUtils.cleanPath(imagen.getOriginalFilename()));
    
    Path destino = directorioBase.resolve(nombreArchivo).normalize();

    // Doble verificación de seguridad
    if (!destino.startsWith(directorioBase)) {
        throw new IOException("Intento de acceso a ruta no permitida: " + destino);
    }

    try (InputStream input = imagen.getInputStream()) {
        Files.copy(input, destino, StandardCopyOption.REPLACE_EXISTING);
    }

    return nombreArchivo;
}


    // Mostrar formulario de edición (solo ADMIN)
    @GetMapping("/admin/productos/editar/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public String mostrarFormularioEdicion(@PathVariable Long id, Model model) {
        Productos producto = productoService.obtenerProductoPorId(id);
        List<Categorias> categorias = categoriaService.listarCategorias();
        
        model.addAttribute("producto", producto);
        model.addAttribute("categorias", categorias);
        return "admin/edit_productos";
    }

   // Método para procesar la actualización (optimizado)
   @PostMapping("/admin/productos/actualizar/{id}")
   @PreAuthorize("hasRole('ADMIN')")
   public String actualizarProducto(
           @PathVariable Long id,
           @ModelAttribute Productos producto,
           @RequestParam(value = "imagenFile", required = false) MultipartFile imagenFile,
           RedirectAttributes redirectAttributes) {
       
       try {
           Productos productoExistente = productoService.obtenerProductoPorId(id);
           
           // Manejo de la imagen
           if (imagenFile != null && !imagenFile.isEmpty()) {
               // Eliminar imagen anterior si existe
               if (productoExistente.getImagen() != null && !productoExistente.getImagen().equals("default-product.png")) {
                   Path rutaAnterior = Paths.get(uploadPath).resolve(productoExistente.getImagen());
                   Files.deleteIfExists(rutaAnterior);
               }
               // Guardar nueva imagen
               String nombreImagen = guardarImagen(imagenFile);
               producto.setImagen(nombreImagen);
           } else {
               // Mantener imagen existente
               producto.setImagen(productoExistente.getImagen());
           }
           
           // Actualizar producto
           productoService.actualizarProducto(id, producto);
           redirectAttributes.addFlashAttribute("success", "Producto actualizado correctamente");
           
       } catch (Exception e) {
           log.error("Error al actualizar producto", e);
           redirectAttributes.addFlashAttribute("error", "Error al actualizar: " + e.getMessage());
           return "redirect:/admin/productos/editar/" + id;
       }
       
       return "redirect:/admin/productos";
   }

    // Eliminar producto (solo ADMIN)
  // Método para eliminar (con confirmación)
  @PostMapping("/admin/productos/eliminar/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  public String eliminarProducto(
          @PathVariable Long id,
          RedirectAttributes redirectAttributes) {
      
      try {
          Productos producto = productoService.obtenerProductoPorId(id);
          
          // Eliminar imagen asociada
          if (producto.getImagen() != null && !producto.getImagen().equals("default-product.png")) {
              Path rutaImagen = Paths.get(uploadPath).resolve(producto.getImagen());
              Files.deleteIfExists(rutaImagen);
          }
          
          productoService.eliminarProducto(id);
          redirectAttributes.addFlashAttribute("success", "Producto eliminado correctamente");
          
      } catch (Exception e) {
          log.error("Error al eliminar producto", e);
          redirectAttributes.addFlashAttribute("error", "Error al eliminar: " + e.getMessage());
      }
      
      return "redirect:/admin/productos";
  }
}
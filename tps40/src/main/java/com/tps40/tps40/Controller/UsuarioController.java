package com.tps40.tps40.Controller;

import com.tps40.tps40.Entities.Rol;
import com.tps40.tps40.Entities.Usuarios;
import com.tps40.tps40.Exceptions.UsuarioNoEncontradoException;
import com.tps40.tps40.Repository.IRolRepository;
import com.tps40.tps40.Repository.IUsuarioRepository;
import com.tps40.tps40.Service.CarritoService;
import com.tps40.tps40.Service.UsuarioService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.util.Map;
import java.util.Optional;
import java.security.Principal;
import java.util.Collection;
import java.util.Collections;

import java.util.List;

@Controller
public class UsuarioController {
    private final UsuarioService usuariosService;
     private final IRolRepository rolRepository;
     private final IUsuarioRepository usuariosRepository;
     
     @Autowired 
      public CarritoService carritoService;

    


    @Autowired
    public UsuarioController(UsuarioService usuariosService, IRolRepository rolRepository,  IUsuarioRepository usuariosRepository)  {
        this.usuariosService = usuariosService;
        this.rolRepository = rolRepository;
        this.usuariosRepository = usuariosRepository;

    }

    // Muestra el formulario de inicio de sesión
    @GetMapping("/usuarios/login")
    public String mostrarLogin(Model model) {
        model.addAttribute("titulo", "Iniciar Sesión");
        return "login";
    }

    // Cierra la sesión del usuario actual
    @GetMapping("/usuarios/logout")
    public String cerrarSesion(HttpSession session) {
        session.invalidate();
        return "redirect:/usuarios/login?logout";
    }

    // Procesa el formulario de registro de nuevos usuarios

    @PostMapping("/usuarios/registro")
    public String procesarRegistro(@ModelAttribute Usuarios usuario, RedirectAttributes redirectAttributes) {
        try {
            Usuarios usuarioGuardado = usuariosService.registrarNuevoUsuario(usuario);
            carritoService.crearCarritoParaUsuario(usuarioGuardado.getId()); // ← Llama desde CarritoService
            redirectAttributes.addFlashAttribute("mensaje", "Registro exitoso");
            return "redirect:/login";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/usuarios/registro";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al registrar usuario: " + e.getMessage());
            return "redirect:/usuarios/registro";
        }
    }


    // Muestra el formulario de registro de nuevos usuarios
    @GetMapping("/usuarios/registro")
    public String mostrarFormularioRegistro(Model model) {
        model.addAttribute("usuario", new Usuarios());
        return "usuarios/registro";
    }

    @GetMapping("/usuarios/perfil")
    public String mostrarPerfilUsuario(Model model, Principal principal) {
        String identificador = principal.getName();

        // Buscar por username o email
        Usuarios usuario = usuariosService.obtenerUsuarioPorIdentificador(identificador);

        model.addAttribute("usuario", usuario);
        return "perfil";
    }

    @GetMapping("/usuarios/editar")
    public String mostrarFormularioEdicion(Model model, Principal principal) {
        // Verificar autenticación primero
        if (principal == null) {
            return "redirect:/usuarios/login";
        }

        try {
            String identificador = principal.getName();
            Usuarios usuario = usuariosService.obtenerUsuarioPorIdentificador(identificador);
            model.addAttribute("usuario", usuario);
            return "editar_perfil";
        } catch (UsuarioNoEncontradoException e) {
            // Registrar el error y redirigir
            return "redirect:/error?mensaje=Usuario no encontrado";
        }
    }

    @PutMapping("/usuarios/actualizar")
    public String actualizarPerfil(
            @ModelAttribute("usuario") Usuarios usuarioActualizado,
            @RequestParam(required = false) String newPassword,
            @RequestParam(required = false) String confirmPassword,
            Principal principal,
            RedirectAttributes redirectAttributes) {

        try {
            // Validar contraseñas
            if (newPassword != null && !newPassword.isEmpty() && !newPassword.equals(confirmPassword)) {
                redirectAttributes.addFlashAttribute("error", "Las contraseñas no coinciden");
                return "redirect:editar_perfil";
            }

            // Obtener usuario actual desde la base de datos
            Usuarios usuarioActual = usuariosService.obtenerUsuarioPorIdentificador(principal.getName());

            // Actualizar usando el ID real
            usuariosService.actualizarUsuario(usuarioActual.getId(), usuarioActualizado, newPassword);

            redirectAttributes.addFlashAttribute("success", "Datos actualizados correctamente");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error: " + e.getMessage());
        }

        return "redirect:perfil";
    }


    // ------------------------ ADMINISTRACIÓN DE USUARIOS ------------------------

    @GetMapping("/admin/usuarios")
    public String listarUsuariosAdmin(Model model) {
        List<Usuarios> usuarios = usuariosService.obtenerTodosUsuarios();
        model.addAttribute("usuarios", usuarios);
        return "admin/usuarios_admin";
    }

    @GetMapping("/admin/usuarios/add")
    @PreAuthorize("hasRole('ADMIN')")
    public String mostrarFormularioAgregarUsuario(Model model) {
        model.addAttribute("usuario", new Usuarios()); // Pasar un objeto vacío para el formulario
        return "admin/add_usuarios"; // Nombre de tu archivo HTML para agregar usuarios
    }
    // Muestra el formulario de edición de usuario
    @GetMapping("/admin/usuarios/edit/{id}")
@PreAuthorize("hasRole('ADMIN')")
public String mostrarFormularioEdicion(@PathVariable Long id, Model model) {
    Usuarios usuario = usuariosService.obtenerUsuarioPorId(id)
            .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
    model.addAttribute("usuario", usuario);
    return "admin/edit_usuarios";
}

    // Procesa la actualización de información de usuario
    // Método para procesar la actualización
    
    @PostMapping("admin/usuarios/actualizar/{id}")
public String actualizarUsuario(
    @PathVariable Long id,
    @ModelAttribute("usuario") Usuarios usuarioActualizado,
    @RequestParam("rolNombre") String rolNombre,
    @RequestParam(required = false) String contraseña,
    RedirectAttributes redirectAttributes) {

    try {
        Rol rol = rolRepository.findByNombreRol(rolNombre)
                .orElseThrow(() -> new IllegalArgumentException("Rol no válido"));
        
        usuarioActualizado.setRol(rol);
        usuariosService.actualizarUsuario(id, usuarioActualizado, contraseña);
        
        redirectAttributes.addFlashAttribute("success", "Usuario actualizado correctamente");
    } catch (IllegalArgumentException e) {
        // Captura nuestros mensajes personalizados
        redirectAttributes.addFlashAttribute("error", e.getMessage());
        return "redirect:/admin/usuarios/edit/" + id;
    } catch (DataIntegrityViolationException e) {
        // Captura cualquier otro error de integridad de la base de datos
        redirectAttributes.addFlashAttribute("error", "Error al actualizar el usuario");
        return "redirect:/admin/usuarios/edit/" + id;
    } catch (Exception e) {
        redirectAttributes.addFlashAttribute("error", "Error inesperado al actualizar el usuario");
        return "redirect:/admin/usuarios/edit/" + id;
    }
    
    return "redirect:/admin/usuarios";
}

@GetMapping("/admin/usuarios/check-username")
@ResponseBody
public Map<String, Boolean> checkUsernameAvailability(
        @RequestParam String username,
        @RequestParam(required = false) Long id) {
    
    boolean exists = usuariosRepository.existsByNombreUsuario(username);
    if (id != null) {
        // Si estamos editando un usuario, verificar si el nombre de usuario pertenece a otro usuario
        Optional<Usuarios> usuario = usuariosRepository.findByNombreUsuario(username);
        exists = usuario.isPresent() && !usuario.get().getId().equals(id);
    }
    
    return Collections.singletonMap("exists", exists);
}

@PostMapping("/admin/usuarios/agregar")
     public String agregarAdministrador(@ModelAttribute Usuarios usuario, RedirectAttributes redirectAttributes, Model model) {
        try {
            // Obtén el rol ADMIN desde la base de datos
            Rol rolAdmin = rolRepository.findByNombreRol("ROLE_ADMIN")
                    .orElseThrow(() -> new RuntimeException("Rol ADMIN no encontrado."));

            usuario.setRol(rolAdmin); // Asigna el rol al usuario

            usuariosService.registrarNuevoAdministrador(usuario);
            redirectAttributes.addFlashAttribute("success", "Usuario Administrador agregado correctamente.");
            return "redirect:/admin/usuarios";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al agregar Usuario Administrador: " + e.getMessage());
            return "redirect:/admin/usuarios/add";
        }
    }


    @PostMapping("/admin/usuarios/eliminar/{id}")
    public String eliminarUsuario(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            usuariosService.eliminarUsuario(id);
            redirectAttributes.addFlashAttribute("success", "Usuario eliminado correctamente.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al eliminar usuario: " + e.getMessage());
        }
        return "redirect:/admin/usuarios";
    }

}
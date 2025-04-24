package com.tps40.tps40.Controller;
import com.tps40.tps40.Entities.Categorias;
import com.tps40.tps40.Service.CategoriasService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/categorias")
public class CategoriasController {
    @Autowired
    private CategoriasService categoriaService;

    // Listar categorías (público)
    @GetMapping
    public String listarCategorias(Model model) {
        model.addAttribute("categorias", categoriaService.listarCategorias());
        return "categorias/lista";
    }

    // Mostrar formulario creación (solo ADMIN)
    @GetMapping("/nuevo")
    @PreAuthorize("hasRole('ADMIN')")
    public String mostrarFormularioCreacion(Model model) {
        model.addAttribute("categoria", new Categorias());
        return "categorias/formulario";
    }

    // Procesar creación (solo ADMIN)
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public String crearCategoria(@ModelAttribute Categorias categoria) {
        categoriaService.crearCategoria(categoria);
        return "redirect:/categorias";
    }

    // Mostrar formulario edición (solo ADMIN)
    @GetMapping("/editar/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public String mostrarFormularioEdicion(@PathVariable Long id, Model model) {
        model.addAttribute("categoria", categoriaService.listarCategorias().stream()
                .filter(c -> c.getId().equals(id))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Categoría no encontrada")));
        return "categorias/formulario";
    }

    // Procesar actualización (solo ADMIN)
    @PostMapping("/actualizar/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public String actualizarCategoria(@PathVariable Long id, @ModelAttribute Categorias categoria) {
        categoriaService.actualizarCategoria(id, categoria);
        return "redirect:/categorias";
    }

    // Eliminar categoría (solo ADMIN)
    @GetMapping("/eliminar/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public String eliminarCategoria(@PathVariable Long id) {
        categoriaService.eliminarCategoria(id);
        return "redirect:/categorias";
    }
}

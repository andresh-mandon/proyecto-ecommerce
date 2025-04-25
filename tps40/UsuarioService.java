package com.tps40.tps40.Service;
import com.tps40.tps40.Entities.Rol;
import com.tps40.tps40.Entities.Usuarios;
import com.tps40.tps40.Events.UsuarioRegistradoEvent;
import com.tps40.tps40.Exceptions.UsuarioNoEncontradoException;
import com.tps40.tps40.Exceptions.UsuarioYaExisteException;
import com.tps40.tps40.Repository.IRolRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.tps40.tps40.Repository.IUsuarioRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;

import java.util.*;

@Service
public class UsuarioService {
    @Autowired
    private ApplicationEventPublisher appliEventPublisher;

    private final IUsuarioRepository usuariosRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final IRolRepository rolRepository;

    @Autowired
    public UsuarioService(IUsuarioRepository usuarioRepository, IRolRepository rolRepository, BCryptPasswordEncoder passwordEncoder) {
        this.usuariosRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.passwordEncoder = passwordEncoder;
    }

    
    @Transactional
    public Usuarios registrarNuevoUsuario(Usuarios usuario) {
        // Validar que el correo y el nombre de usuario no existan
        if (usuariosRepository.existsByEmail(usuario.getEmail())) {
            throw new IllegalArgumentException("El correo electrónico ya está registrado.");
        }
        if (usuariosRepository.existsByNombreUsuario(usuario.getNombreUsuario())) {
            throw new IllegalArgumentException("El nombre de usuario ya está en uso.");
        }

        // Encriptar la contraseña
        String encodedPassword = passwordEncoder.encode(usuario.getContraseña());
        usuario.setContraseña(encodedPassword);

        // Asignar el rol de USUARIO
        Rol rolUsuario = rolRepository.findByNombreRol("ROLE_USUARIO")
                .orElseThrow(() -> new RuntimeException("Rol USUARIO no encontrado."));
        usuario.setRol(rolUsuario);

        Usuarios usuarioGuardado = usuariosRepository.save(usuario);

        // Disparar el evento
        appliEventPublisher.publishEvent(new UsuarioRegistradoEvent(this, usuarioGuardado));

        // Guardar el usuario
        return usuarioGuardado;
    }


    //Registrar Usuario Administrador
    @Transactional
    public Usuarios registrarNuevoAdministrador(Usuarios usuario) {
        if (usuariosRepository.existsByEmail(usuario.getEmail())) {
            throw new UsuarioYaExisteException("El correo electrónico ya está registrado.");
        }
        if (usuariosRepository.existsByNombreUsuario(usuario.getNombreUsuario())) {
            throw new UsuarioYaExisteException("El nombre de usuario ya está en uso.");
        }

        String encodedPassword = passwordEncoder.encode(usuario.getContraseña());
        usuario.setContraseña(encodedPassword);

        // Asignar el rol de ADMIN
        Rol rolAdmin = rolRepository.findByNombreRol("ROLE_ADMIN")
                .orElseThrow(() -> new RuntimeException("Rol ADMIN no encontrado.")); // Considera una excepción personalizada aquí
        usuario.setRol(rolAdmin);

        return usuariosRepository.save(usuario);
    }

    @Transactional(readOnly = true, isolation = Isolation.READ_COMMITTED)
    public Usuarios obtenerUsuarioPorIdentificador(String identificador) {
        Optional<Usuarios> usuarioOpt = usuariosRepository.findByNombreUsuario(identificador);
        if (identificador == null || identificador.isBlank()) {
            throw new IllegalArgumentException("Identificador inválido");
        }

        return usuariosRepository.findByNombreUsuario(identificador)
                .or(() -> usuariosRepository.findByEmail(identificador))
                .orElseThrow(() -> new UsuarioNoEncontradoException("Credenciales no válidas"));
    }

    @Transactional(readOnly = true)
    public List<Usuarios> obtenerTodosUsuarios() {
        return usuariosRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Usuarios obtenerUsuarioPorNombreUsuario(String nombreUsuario) {
        return usuariosRepository.findByNombreUsuario(nombreUsuario)
                .orElseThrow(() -> new UsuarioNoEncontradoException("Usuario no encontrado"));
    }

    @Transactional(readOnly = true)
    public Optional<Usuarios> obtenerUsuarioPorId(Long id) {
        return usuariosRepository.findById(id);
    }

    @Transactional
    public void actualizarUsuario(Long id, Usuarios usuarioActualizado, String contraseña) {
        Usuarios usuarioExistente = usuariosRepository.findById(id)
                .orElseThrow(() -> new UsuarioNoEncontradoException("Usuario con ID " + id + " no encontrado"));

        // Validar unicidad del email
        if (!usuarioExistente.getEmail().equals(usuarioActualizado.getEmail())
                && usuariosRepository.existsByEmail(usuarioActualizado.getEmail())) {
            throw new UsuarioYaExisteException("El email " + usuarioActualizado.getEmail() + " ya está registrado");
        }

        // Validar unicidad del nombre de usuario
        if (!usuarioExistente.getNombreUsuario().equals(usuarioActualizado.getNombreUsuario())
                && usuariosRepository.existsByNombreUsuario(usuarioActualizado.getNombreUsuario())) {
            throw new UsuarioYaExisteException("El usuario " + usuarioActualizado.getNombreUsuario() + " ya existe");
        }

        // Actualizar campos básicos
        usuarioExistente.setNombre(usuarioActualizado.getNombre());
        usuarioExistente.setApellido(usuarioActualizado.getApellido());
        usuarioExistente.setEmail(usuarioActualizado.getEmail());
        usuarioExistente.setNombreUsuario(usuarioActualizado.getNombreUsuario());
        usuarioExistente.setContacto(usuarioActualizado.getContacto());
        usuarioExistente.setDireccion(usuarioActualizado.getDireccion());

        // Actualizar rol si es necesario
        if (usuarioActualizado.getRol() != null) {
            usuarioExistente.setRol(usuarioActualizado.getRol());
        }

        // Actualizar contraseña si se proporciona
        if (contraseña != null && !contraseña.isEmpty()) {
            usuarioExistente.setContraseña(passwordEncoder.encode(contraseña));
        }

        // Guardar cambios explícitamente (aunque no es necesario con @Transactional)
        usuariosRepository.save(usuarioExistente);
    }


    @Transactional
    public void eliminarUsuario(Long id) {
        usuariosRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public Optional<Usuarios> autenticarUsuario(String identificador, String contraseña) {
        Optional<Usuarios> usuarioOpt = usuariosRepository.findByNombreUsuario(identificador);


        if (!usuarioOpt.isPresent()) {
            usuarioOpt = usuariosRepository.findByEmail(identificador);
        }

        return usuarioOpt.filter(usuario -> passwordEncoder.matches(contraseña, usuario.getContraseña()));
    }
}
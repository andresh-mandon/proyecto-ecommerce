package com.tps40.tps40.Service;

import com.tps40.tps40.Entities.Rol;
import com.tps40.tps40.Entities.Usuarios;
import com.tps40.tps40.Repository.IUsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private IUsuarioRepository usuarioRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Optional<Usuarios> usuarioOptional = usuarioRepository.findByEmail(email);

        if (usuarioOptional.isEmpty()) {
            throw new UsernameNotFoundException("Usuario no encontrado con el email: " + email);
        }

        Usuarios usuario = usuarioOptional.get(); // Obtiene el Usuario del Optional (sabemos que existe)
        Rol rol = usuario.getRol();
        List<GrantedAuthority> authorities = Collections.singletonList(new SimpleGrantedAuthority(rol.getNombreRol()));

        return new User(usuario.getEmail(), usuario.getContraseña(), authorities);
    }
}
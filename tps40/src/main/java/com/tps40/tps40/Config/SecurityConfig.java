package com.tps40.tps40.Config;

import jakarta.servlet.ServletException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collection;

@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationSuccessHandler authenticationSuccessHandler() {
        return new SimpleUrlAuthenticationSuccessHandler() {
            @Override
            public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                                Authentication authentication) throws IOException, ServletException {
                String redirectUrl = "/"; // Default
                Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
                for (GrantedAuthority authority : authorities) {
                    if (authority.getAuthority().equals("ROLE_ADMIN")) {
                        redirectUrl = "/admin/dashboard";
                        break;
                    }
                }
                setDefaultTargetUrl(redirectUrl);
                super.onAuthenticationSuccess(request, response, authentication);
            }
        };
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http

                .authorizeHttpRequests(auth -> auth
                        // Rutas públicas
                        .requestMatchers(
                                "/",
                                "/home",
                                "/usuarios/registro",
                                "/usuarios/login",
                                "/productos",
                                "/productos/buscar",
                                "/categorias",
                                "/css/**",
                                "/js/**",
                                "/images/**"
                        ).permitAll()

                        // Rutas para usuarios autenticados
                        .requestMatchers(HttpMethod.PUT, "/usuarios/actualizar",
                                "/carrito",
                                "/carrito/**",
                                "/ordenes/mis-ordenes",
                                "/ordenes/crear",
                                "/usuarios/perfil",
                                "/usuarios/editar",
                                "/usuarios/actualizar"
                        ).authenticated()

                        .requestMatchers(HttpMethod.POST, "/carrito/agregar").authenticated()

                        // Rutas administrativas
                        .requestMatchers(
                                "/usuarios/lista",
                                "/usuarios/editar/*",
                                "/usuarios/eliminar/*",
                                "/productos/nuevo",
                                "/productos/editar/*",
                                "/productos/eliminar/*",
                                "/categorias/nuevo",
                                "/categorias/editar/*",
                                "/categorias/eliminar/*",
                                "/ordenes/*/estado",
                                "/admin/**"
                        ).hasRole("ADMIN")

                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .usernameParameter("username") // o "email" según tu formulario
                        .passwordParameter("password")
                        .loginPage("/usuarios/login")
                        .successHandler(authenticationSuccessHandler()) // Usar el handler personalizado
                        .permitAll()
                )
                .logout(logout -> logout
                .logoutUrl("/usuarios/logout") // Esta es la URL que configuraste
                .logoutSuccessUrl("/usuarios/login?logout")
                .permitAll()
);
        return http.build();
    }
}

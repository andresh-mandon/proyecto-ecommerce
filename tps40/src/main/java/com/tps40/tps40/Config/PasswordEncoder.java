package com.tps40.tps40.Config;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class PasswordEncoder {
    public static void main(String[] args) {
        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        String rawPassword = "andresh"; // Reemplaza con la contraseña real
        String encodedPassword = passwordEncoder.encode(rawPassword);
        System.out.println("Contraseña encriptada: " + encodedPassword);
        // ¡Usa esta contraseña encriptada en tu INSERT SQL!
    }
}

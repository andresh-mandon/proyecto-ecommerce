package com.tps40.tps40.Config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

import jakarta.annotation.PostConstruct;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays; // Para imprimir bytes

@Configuration
public class MvcConfig implements WebMvcConfigurer {

    private static final Logger log = LoggerFactory.getLogger(MvcConfig.class);

    @Value("${file.upload-dir}")
private String uploadPath;

    @PostConstruct
    public void init() {
        try {
            Path uploadDir = Paths.get(uploadPath).toAbsolutePath().normalize();

            
            // Verificación adicional de seguridad
            if (!uploadDir.startsWith(Paths.get(System.getProperty("user.home")).normalize())) {
                throw new SecurityException("La ruta de uploads debe estar dentro del home del usuario");
            }

            if (!Files.exists(uploadDir)) {
                Files.createDirectories(uploadDir);
                log.info("Directorio creado: {}", uploadDir);
            }
            
            // Verificar permisos
            if (!Files.isWritable(uploadDir)) {
                throw new IOException("No hay permisos de escritura en: " + uploadDir);
            }

        } catch (Exception e) {
            log.error("Error crítico al inicializar uploads", e);
            throw new RuntimeException("No se pudo inicializar el directorio de uploads", e);
        }
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path uploadDir = Paths.get(uploadPath).toAbsolutePath().normalize();
        String resourceLocation = "file:" + uploadDir + "/";
        
        log.info("Configurando recursos estáticos en: {}", resourceLocation);
        
        registry.addResourceHandler("/uploads/**")
               .addResourceLocations(resourceLocation)
               .setCachePeriod(3600)
               .resourceChain(true)
               .addResolver(new PathResourceResolver());
        
        registry.addResourceHandler("/static/**")
               .addResourceLocations("classpath:/static/");
    }
}
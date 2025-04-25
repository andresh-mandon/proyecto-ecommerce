package com.tps40.tps40.Repository;

import com.tps40.tps40.Entities.Rol;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface IRolRepository  extends JpaRepository<Rol, Long> {
    Optional<Rol> findByNombreRol(String nombreRol);

}

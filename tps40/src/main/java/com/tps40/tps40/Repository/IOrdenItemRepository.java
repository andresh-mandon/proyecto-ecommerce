package com.tps40.tps40.Repository;
import com.tps40.tps40.Entities.Orden;
import com.tps40.tps40.Entities.OrdenItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IOrdenItemRepository extends JpaRepository<OrdenItem, Long> {
    List<OrdenItem> findByOrden(Orden orden);
}

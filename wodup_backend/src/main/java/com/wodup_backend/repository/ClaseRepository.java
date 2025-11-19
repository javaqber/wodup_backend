package com.wodup_backend.repository;

import com.wodup_backend.model.Clase;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.time.LocalDate;

public interface ClaseRepository extends JpaRepository<Clase, Long> {
    List<Clase> findByFechaGreaterThanEqualOrderByFechaAscHoraInicioAsc(LocalDate fecha);

    List<Clase> findByFechaOrderByHoraInicioAsc(LocalDate fecha);

    List<Clase> findByFecha(LocalDate fecha);

    boolean existsByFecha(LocalDate fecha);
}

package com.wodup_backend.service;

import com.wodup_backend.model.Clase;
import com.wodup_backend.model.Usuario;
import com.wodup_backend.payload.request.ClaseCreationRequest;
import com.wodup_backend.repository.ClaseRepository;
import com.wodup_backend.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDate;
import java.util.List;

@Service
public class ClaseService {

    @Autowired
    private ClaseRepository claseRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    // 1. Obtener todas las clases futuras
    public List<Clase> getClasesFuturas() {
        return claseRepository.findByFechaGreaterThanEqualOrderByFechaAscHoraInicioAsc(LocalDate.now());
    }

    // 2. Crear una nueva clase (Usando el DTO y el ID del Coach del token)
    @Transactional // Marca el método como una operación transaccional
    public Clase crearClase(ClaseCreationRequest request, Long coachId) {

        // Verificar y obtener el Coach
        Usuario coach = usuarioRepository.findById(coachId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Coach no encontrado con ID: " + coachId));

        // Validación: Solo una clase por dia
        boolean existeClaseEseDia = claseRepository.existsByFecha(request.getFecha());
        if (existeClaseEseDia) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Ya existe una clase programada para este día.");

        }

        // Validación de Horas
        if (request.getHoraFin().isBefore(request.getHoraInicio())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La hora de fin debe ser posterior a la hora de inicio.");
        }

        // Mapear DTO a Entidad
        Clase nuevaClase = new Clase();
        nuevaClase.setNombre(request.getNombre());
        nuevaClase.setFecha(request.getFecha());
        nuevaClase.setHoraInicio(request.getHoraInicio());
        nuevaClase.setHoraFin(request.getHoraFin());
        nuevaClase.setCapacidad(request.getCapacidad());
        nuevaClase.setCoach(coach);

        // Guardar y devolver
        return claseRepository.save(nuevaClase);
    }

    // 3. Actualizar una clase
    public Clase updateClase(Long id, Clase claseDetails) {
        Clase clase = claseRepository.findById(id)
                .orElseThrow(
                        () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Clase no encontrada con ID: " + id));
        clase.setNombre(claseDetails.getNombre());
        clase.setFecha(claseDetails.getFecha());
        clase.setHoraInicio(claseDetails.getHoraInicio());
        clase.setHoraFin(claseDetails.getHoraFin());
        clase.setCapacidad(claseDetails.getCapacidad());

        return claseRepository.save(clase);
    }

    // 4. Eliminar una clase
    public void eliminarClase(Long id) {
        if (!claseRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Clase no encontrada con ID: " + id);
        }
        claseRepository.deleteById(id);
    }

    public List<Clase> getClasesDeHoy() {
        LocalDate hoy = LocalDate.now();
        return claseRepository.findByFechaOrderByHoraInicioAsc(hoy);
    }
}
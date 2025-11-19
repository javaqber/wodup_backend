package com.wodup_backend.Controller;

import com.wodup_backend.model.Clase;
import com.wodup_backend.payload.request.ClaseCreationRequest;
import com.wodup_backend.dto.ClaseDTO;
import com.wodup_backend.service.UserDetailsImpl;
import com.wodup_backend.service.ClaseService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@CrossOrigin(origins = "http://localhost:4200", maxAge = 3600)
@RestController
@RequestMapping("/api/clases")
public class ClaseController {

    @Autowired
    private ClaseService claseService;

    // 1. OBTENER CLASES (Futuras)
    @GetMapping
    public ResponseEntity<List<ClaseDTO>> obtenerClasesFuturas() {
        List<Clase> clases = claseService.getClasesFuturas();

        // Mapeamos la lista de Entidad (Clase) a DTO (ClaseDTO) para la respuesta
        List<ClaseDTO> clasesDTO = clases.stream()
                .map(clase -> new ClaseDTO(
                        clase.getId(),
                        clase.getNombre(),
                        clase.getFecha(),
                        clase.getHoraInicio(),
                        clase.getHoraFin(),
                        clase.getCapacidad(),
                        clase.getCoach() != null ? clase.getCoach().getId() : null))
                .collect(Collectors.toList());

        return ResponseEntity.ok(clasesDTO);
    }

    // 2. CREAR CLASE (Solo para Coaches/Admins) - Método Actualizado
    // Usamos el rol "COACH" en SecurityConfig, pero se mantiene la anotación por si
    // decides usarla
    @PreAuthorize("hasRole('ADMIN') or hasRole('COACH')")
    @PostMapping
    public ResponseEntity<ClaseDTO> crearClase(
            @Valid @RequestBody ClaseCreationRequest request, // Usamos el DTO de request
            Authentication authentication) { // Obtenemos el contexto de autenticación

        // 1. Obtener el ID del Coach autenticado
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        Long coachId = userDetails.getId();

        // 2. Crear la clase usando el DTO y el ID del coach
        Clase nuevaClase = claseService.crearClase(request, coachId);

        // 3. Mapear la Entidad guardada a un DTO de respuesta
        ClaseDTO responseDTO = new ClaseDTO(
                nuevaClase.getId(),
                nuevaClase.getNombre(),
                nuevaClase.getFecha(),
                nuevaClase.getHoraInicio(),
                nuevaClase.getHoraFin(),
                nuevaClase.getCapacidad(),
                nuevaClase.getCoach().getId());

        // 4. Devolver 201 Created
        return ResponseEntity.status(HttpStatus.CREATED).body(responseDTO);
    }

    // 3. ACTUALIZAR CLASE
    @PreAuthorize("hasRole('ADMIN') or hasRole('COACH')")
    @PutMapping("/{id}")
    public ResponseEntity<Clase> updateClase(@PathVariable Long id, @RequestBody Clase claseDetails) {
        Clase claseActualizada = claseService.updateClase(id, claseDetails);
        return ResponseEntity.ok(claseActualizada);
    }

    // 4. ELIMINAR CLASE
    @PreAuthorize("hasRole('ADMIN') or hasRole('COACH')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarClase(@PathVariable Long id) {
        claseService.eliminarClase(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/hoy")
    public ResponseEntity<List<ClaseDTO>> obtenerClasesDeHoy() {
        List<Clase> clasesHoy = claseService.getClasesDeHoy();

        List<ClaseDTO> clasesDTO = clasesHoy.stream()
                .map(clase -> new ClaseDTO(
                        clase.getId(),
                        clase.getNombre(),
                        clase.getFecha(),
                        clase.getHoraInicio(),
                        clase.getHoraFin(),
                        clase.getCapacidad(),
                        clase.getCoach() != null ? clase.getCoach().getId() : null))
                .collect(Collectors.toList());

        return ResponseEntity.ok(clasesDTO);
    }

}
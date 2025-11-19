package com.wodup_backend.Controller;

import com.wodup_backend.model.Reserva;
import com.wodup_backend.model.Usuario;
import com.wodup_backend.repository.UsuarioRepository;
import com.wodup_backend.service.ReservaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@CrossOrigin(origins = "http://localhost:4200", maxAge = 3600)
@RestController
@RequestMapping("/api/reservas")
@PreAuthorize("isAuthenticated()") // Todas las rutas requieren usuario logueado
public class ReservaController {

    @Autowired
    private ReservaService reservaService;

    // INYECTAMOS el UsuarioRepository para buscar el ID real del usuario logueado
    @Autowired
    private UsuarioRepository usuarioRepository;

    private Usuario getAtletaFromUserDetails(UserDetails userDetails) {
        String email = userDetails.getUsername();
        return usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Usuario autenticado no encontrado en la base de datos con email: " + email));
    }

    // 1. OBTENER MIS RESERVAS (Solo para el atleta logueado)
    @GetMapping("/mis")
    @PreAuthorize("hasRole('ATHLETE')")
    public ResponseEntity<List<Reserva>> getMisReservas(@AuthenticationPrincipal UserDetails userDetails) {

        // Búsqueda del ID real del atleta
        Usuario atleta = getAtletaFromUserDetails(userDetails);
        Long atletaId = atleta.getId();

        List<Reserva> reservas = reservaService.getReservasByAtletaId(atletaId);
        return ResponseEntity.ok(reservas);
    }

    // 2. CREAR RESERVA (Requiere ID de clase en el path)
    @PostMapping
    @PreAuthorize("hasRole('ATHLETE')")
    public ResponseEntity<Reserva> reservarClase(
            @RequestBody com.wodup_backend.dto.ReservaDTO reservaDTO,
            @AuthenticationPrincipal UserDetails userDetails) {

        String usuarioEmail = userDetails.getUsername();

        try {
            Reserva reserva = reservaService.crearReserva(
                    usuarioEmail,
                    reservaDTO.getClaseId(),
                    reservaDTO.getHoraInicio(),
                    reservaDTO.getHoraFin());
            return new ResponseEntity<>(reserva, HttpStatus.CREATED);
        } catch (ResponseStatusException e) {
            throw e;
        }
    }

    // 3. CANCELAR RESERVA
    @DeleteMapping("/{reservaId}")
    @PreAuthorize("hasRole('ATHLETE')")
    public ResponseEntity<Void> cancelarReserva(@PathVariable Long reservaId,
            @AuthenticationPrincipal UserDetails userDetails) {

        // Búsqueda del ID real del atleta
        Usuario atleta = getAtletaFromUserDetails(userDetails);
        Long atletaId = atleta.getId();

        reservaService.cancelarReserva(reservaId, atletaId);
        return ResponseEntity.noContent().build();
    }
}
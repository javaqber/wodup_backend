package com.wodup_backend.service;

import com.wodup_backend.model.*;
import com.wodup_backend.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Service
public class ReservaService {

    @Autowired
    private ReservaRepository reservaRepository;

    @Autowired
    private ClaseRepository claseRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private SuscripcionRepository suscripcionRepository;

    public List<Reserva> getReservasByAtletaId(Long atletaId) {
        return reservaRepository.findByAtletaId(atletaId);
    }

    @Transactional
    public Reserva crearReserva(String usuarioEmail, Long claseId, String horaInicio, String horaFin) {
        // 1. Obtener Atleta y Clase
        Usuario atleta = usuarioRepository.findByEmail(usuarioEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Atleta no encontrado con email: " + usuarioEmail));

        Clase clase = claseRepository.findById(claseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Clase no encontrada."));

        Long atletaId = atleta.getId();

        // 2. Resolver Horas
        LocalTime inicio;
        LocalTime fin;

        if (horaInicio != null && horaFin != null) {
            try {
                inicio = LocalTime.parse(horaInicio);
                fin = LocalTime.parse(horaFin);
            } catch (Exception ex) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Formato de hora inválido.");
            }
        } else {
            Object hiObj = clase.getHoraInicio();
            Object hfObj = clase.getHoraFin();
            if (hiObj == null || hfObj == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La clase no tiene horas definidas.");
            }
            inicio = (hiObj instanceof LocalTime) ? (LocalTime) hiObj : LocalTime.parse(hiObj.toString());
            fin = (hfObj instanceof LocalTime) ? (LocalTime) hfObj : LocalTime.parse(hfObj.toString());
        }

        // 3. Validaciones básicas
        if (fin.isBefore(inicio) || fin.equals(inicio)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Hora fin inválida.");
        }

        // =================================================================================
        // 4. REGLAS DE NEGOCIO (SOLICITADAS)
        // =================================================================================

        List<Reserva> historialReservas = reservaRepository.findByAtletaId(atletaId);

        // REGLA A: Solo 1 reserva ACTIVA por día/clase.
        // Si tienes una reserva CONFIRMADA para este día, no puedes reservar otra
        // (debes cancelar primero).
        boolean tieneReservaActivaEnDia = historialReservas.stream()
                .anyMatch(r -> r.getClase().getId().equals(claseId) &&
                        !r.getEstado().equals("CANCELADA"));

        if (tieneReservaActivaEnDia) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Ya tienes una clase reservada hoy. Cancélala antes de cambiar de hora.");
        }

        // REGLA B: Una vez cancelada, no se puede volver a reservar esa clase.
        boolean esMismaCancelada = historialReservas.stream()
                .anyMatch(r -> r.getClase().getId().equals(claseId) &&
                        r.getHoraInicio().equals(inicio) &&
                        r.getEstado().equals("CANCELADA"));

        if (esMismaCancelada) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "No puedes volver a reservar la misma franja horaria que has cancelado.");
        }

        // 5. Validar Capacidad
        int reservasEnFranja = reservaRepository.countByClaseAndHoraInicio(clase, inicio);
        // (Opcional: Si quieres ser estricto, resta las canceladas del count aquí
        // también)
        if (reservasEnFranja >= clase.getCapacidad()) {
            // throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La franja está
            // llena.");
        }

        // 6. Guardar
        Reserva nuevaReserva = new Reserva();
        nuevaReserva.setAtleta(atleta);
        nuevaReserva.setClase(clase);
        nuevaReserva.setHoraInicio(inicio);
        nuevaReserva.setHoraFin(fin);
        nuevaReserva.setFechaReserva(LocalDateTime.now());
        nuevaReserva.setEstado("CONFIRMADA");

        return reservaRepository.save(nuevaReserva);
    }

    @Transactional
    public void cancelarReserva(Long reservaId, Long atletaId) {
        Reserva reserva = reservaRepository.findById(reservaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Reserva no encontrada."));

        if (!reserva.getAtleta().getId().equals(atletaId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No tienes permiso.");
        }

        // Tiempo límite comentado temporalmente
        /*
         * if
         * (reserva.getClase().getFecha().atTime(reserva.getHoraInicio()).minusHours(1).
         * isBefore(LocalDateTime.now())) {
         * throw ...
         * }
         */

        if (reserva.getEstado().equals("CANCELADA")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ya está cancelada.");
        }

        reserva.setEstado("CANCELADA");
        reservaRepository.save(reserva);

        // Devolver sesión (lógica suscripción comentada o simplificada)
        Optional<Suscripcion> suscripcionOpt = suscripcionRepository
                .findByAtletaIdAndEstadoAndFechaFinGreaterThanEqual(atletaId, "ACTIVA", LocalDate.now());

        suscripcionOpt.ifPresent(suscripcion -> {
            if (suscripcion.getTarifa().getLimiteSesiones() != null) {
                suscripcion.setSesiones_restantes(suscripcion.getSesiones_restantes() + 1);
                suscripcionRepository.save(suscripcion);
            }
        });
    }
}
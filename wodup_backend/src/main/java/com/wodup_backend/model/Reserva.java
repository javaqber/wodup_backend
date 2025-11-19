package com.wodup_backend.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.time.LocalTime;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Entity
@Table(name = "Reserva")
@Data
@NoArgsConstructor
@AllArgsConstructor
// Excluimos las entidades relacionadas para evitar recursión en toString y
// hashCode/equals
@ToString(exclude = { "atleta", "clase" })
@EqualsAndHashCode(exclude = { "atleta", "clase" })
public class Reserva {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Columna para la fecha de reserva
    @Column(name = "fecha_reserva", nullable = false)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") // Formato legible en el JSON
    private LocalDateTime fechaReserva = LocalDateTime.now();

    @Column(nullable = false)
    @JsonFormat(pattern = "HH:mm:ss") // Convierte [19, 0] a "19:00:00"
    private LocalTime horaInicio;

    @Column(nullable = false)
    @JsonFormat(pattern = "HH:mm:ss") // Convierte [20, 0] a "20:00:00"
    private LocalTime horaFin;

    // Campo para el estado (CONFIRMADA, CANCELADA, etc.)
    @Column(name = "estado", nullable = false)
    private String estado = "PENDIENTE";

    // Relación N:1 con Usuario (el atleta que reserva)
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "atleta_id", nullable = false)
    @JsonIgnoreProperties({ "password", "roles", "reservas" }) // Evita bucles infinitos pero envía el nombre
    private Usuario atleta;

    // Relación N:1 con Clase (la clase reservada)
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "clase_id", nullable = false)
    @JsonIgnoreProperties("reservas")
    private Clase clase;

    // Constructor para crear una reserva de forma sencilla
    public Reserva(Usuario atleta, Clase clase) {
        this.atleta = atleta;
        this.clase = clase;
        this.fechaReserva = LocalDateTime.now();
        this.estado = "CONFIRMADA"; // Estado inicial al crear una reserva
    }
}
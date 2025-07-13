package com.example.peloteros.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.peloteros.dao.ReservaRepository;
import com.example.peloteros.model.Cancha;
import com.example.peloteros.model.Reserva;
import com.example.peloteros.model.Usuario;

import java.util.List;

@Service
public class ReservaService {

    private final ReservaRepository reservaRepository;

    @Autowired
    public ReservaService(ReservaRepository reservaRepository) {
        this.reservaRepository = reservaRepository;
    }

    public Reserva crearReserva(Reserva reserva) {
        return reservaRepository.save(reserva);
    }

    public List<Reserva> obtenerReservasPorUsuario(Usuario usuario) {
        return reservaRepository.findByUsuario(usuario);
    }

    public void cancelarReserva(Long reservaId) {
        reservaRepository.deleteById(reservaId);
    }

    public List<Reserva> obtenerReservasActivasPorUsuario(Usuario usuario) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from
        // nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    public List<Reserva> obtenerReservasPasadasPorUsuario(Usuario usuario) {
        return reservaRepository.findByUsuarioAndFechaHoraFinBeforeAndEstadoNot(
                usuario, LocalDateTime.now(), "CANCELADA");
    }

    public List<Reserva> obtenerReservasCanceladasPorUsuario(Usuario usuario) {
        return reservaRepository.findByUsuarioAndEstado(usuario, "CANCELADA");
    }

    public Reserva obtenerReservaPorId(Long id) {
        return reservaRepository.findById(id).orElse(null);
    }

    public boolean validarDisponibilidad(Cancha cancha, LocalDateTime fechaHoraInicio, LocalDateTime fechaHoraFin) {
        // Verificar si hay reservas que se superpongan con el horario solicitado
        List<Reserva> reservasExistentes = reservaRepository
                .findByCanchaAndFechaHoraInicioLessThanAndFechaHoraFinGreaterThanAndEstadoNot(
                        cancha, fechaHoraFin, fechaHoraInicio, "CANCELADA");
        return reservasExistentes.isEmpty();
    }

    public List<Reserva> obtenerReservasPorUsuarioYEstado(Usuario usuario, String estado) {
        return reservaRepository.findByUsuarioAndEstado(usuario, estado);
    }

    public List<Reserva> obtenerReservasPasadas(Usuario usuario) {
        return reservaRepository.findByUsuarioAndFechaHoraFinBeforeAndEstadoNot(
                usuario, LocalDateTime.now(), "CANCELADA");
    }

    public void cancelarReserva(Long reservaId, String motivo) {
        Reserva reserva = obtenerReservaPorId(reservaId);
        if (reserva != null) {
            reserva.setEstado("CANCELADA");
            if (motivo != null && !motivo.trim().isEmpty()) {
                reserva.setComentarios(motivo);
            }
            reservaRepository.save(reserva);
        } else {
            throw new RuntimeException("Reserva no encontrada");
        }
    }
    public long contarReservasHoy() {
        LocalDate hoy = LocalDate.now();
        LocalDateTime inicio = hoy.atStartOfDay();
        LocalDateTime fin = hoy.atTime(LocalTime.MAX);
        return reservaRepository.countByFechaHoraInicioBetweenAndEstadoNot(inicio, fin, "CANCELADA");
    }
     public List<Reserva> obtenerReservasPorCanchaYFecha(Cancha cancha, LocalDate fecha) {
        LocalDateTime inicio = fecha.atStartOfDay();
        LocalDateTime fin = fecha.atTime(23, 59, 59);
        return reservaRepository.findByCanchaAndFechaHoraInicioBetween(cancha, inicio, fin);
    }
    public List<Reserva> obtenerReservasPorFecha(LocalDate fecha) {
        LocalDateTime inicio = fecha.atStartOfDay();
        LocalDateTime fin = fecha.atTime(23, 59, 59);
        return reservaRepository.findByFechaHoraInicioBetween(inicio, fin);
    }
     public void cambiarEstado(Long id, String nuevoEstado) {
        Reserva reserva = reservaRepository.findById(id).orElseThrow(() ->
            new IllegalArgumentException("Reserva no encontrada con ID: " + id)
        );
        reserva.setEstado(nuevoEstado);
        reservaRepository.save(reserva);
    }
    

}

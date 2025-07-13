



/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.example.peloteros.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import com.example.peloteros.model.Cancha;
import com.example.peloteros.model.Reserva;
import com.example.peloteros.model.Usuario;
import com.example.peloteros.service.CanchaService;
import com.example.peloteros.service.ReservaService;
import com.example.peloteros.service.UsuarioService;

import jakarta.servlet.http.HttpSession; // Keep for other methods for now
import java.security.Principal;
import org.springframework.web.servlet.mvc.support.RedirectAttributes; // Added import
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/reserva") // Changed back to /reserva
public class ReservaController {

    private final CanchaService canchaService;
    private final ReservaService reservaService;
    private final UsuarioService usuarioService;

    @Autowired
    public ReservaController(CanchaService canchaService,
            ReservaService reservaService,
            UsuarioService usuarioService) {
        this.canchaService = canchaService;
        this.reservaService = reservaService;
        this.usuarioService = usuarioService;
    }

    @GetMapping("/reservar") // This will now map to /reservar
    public String reservar(Model model, Principal principal) {
        if (principal == null) {
            // Should not happen if security is configured correctly for this path
            // and this path requires authentication
            return "redirect:/usuario/login";
        }
        String email = principal.getName(); // This is the username (email)
        Optional<com.example.peloteros.model.Usuario> usuarioOptional = usuarioService.obtenerUsuarioPorEmail(email);

        if (usuarioOptional.isEmpty()) {
            // Handle error: user not found in DB despite being authenticated
            // This might indicate a data consistency issue or a problem with UserDetailsService logic
            return "redirect:/usuario/login?error=usernotfound";
        }
        com.example.peloteros.model.Usuario usuario = usuarioOptional.get();
        model.addAttribute("usuario", usuario);

        // Obtener las canchas disponibles
        List<Cancha> canchas = canchaService.obtenerTodasCanchas();
        model.addAttribute("canchas", canchas);
        return "reservar"; // Renders reservar.html
    }

    @PostMapping("/reservar") // This will now map to /reservar (POST)
    public String realizarReserva(
            @RequestParam Long canchaId,
            @RequestParam String fecha,
            @RequestParam String horaInicio,
            @RequestParam String horaFin,
            Principal principal, // Added Principal
            RedirectAttributes redirectAttributes, // Added RedirectAttributes
            Model model) {

        if (principal == null) {
            // Safeguard, Spring Security should handle unauthorized access
            return "redirect:/usuario/login"; 
        }
        String username = principal.getName();
        Optional<com.example.peloteros.model.Usuario> usuarioOpt = usuarioService.obtenerUsuarioPorEmail(username);

        if (!usuarioOpt.isPresent()) {
            // Authenticated principal but user not found in DB.
            redirectAttributes.addFlashAttribute("errorMessage", "Error: Usuario autenticado no encontrado.");
            return "redirect:/reserva/reservar"; // Redirect back to reservation form with error
        }
        com.example.peloteros.model.Usuario usuario = usuarioOpt.get();

        Cancha cancha = canchaService.obtenerCanchaPorId(canchaId);
        if (cancha == null) {
            model.addAttribute("error", "Cancha no encontrada"); // This error should be a flash attribute if redirecting
            redirectAttributes.addFlashAttribute("errorMessage", "Cancha no encontrada.");
            return "redirect:/reserva/reservar";
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        LocalDateTime fechaHoraInicio = LocalDateTime.parse(fecha + " " + horaInicio, formatter);
        LocalDateTime fechaHoraFin = LocalDateTime.parse(fecha + " " + horaFin, formatter);

        // Calcular duración en horas
        long horas = java.time.Duration.between(fechaHoraInicio, fechaHoraFin).toHours();
        double precioTotal = cancha.getPrecioPorHora() * horas;

        if (!reservaService.validarDisponibilidad(cancha, fechaHoraInicio, fechaHoraFin)) {
            redirectAttributes.addFlashAttribute("error", "La cancha ya está reservada en este horario.");
            return "redirect:/reserva/reservar";
        }

        Reserva reserva = new Reserva();
        reserva.setUsuario(usuario);
        reserva.setCancha(cancha);
        reserva.setFechaHoraInicio(fechaHoraInicio);
        reserva.setFechaHoraFin(fechaHoraFin);
        reserva.setPrecioTotal(precioTotal);
        reserva.setEstado("PENDIENTE");

        reservaService.crearReserva(reserva);

        return "redirect:/reserva/confirmacion?reservaId=" + reserva.getId();
    }

    @GetMapping("/misreservas")
    public String misReservas(Model model, Principal principal, RedirectAttributes redirectAttributes) {
        if (principal == null) {
            // Should be caught by Spring Security if path is protected, but as a safeguard
            return "redirect:/usuario/login";
        }
        String email = principal.getName();
        java.util.Optional<com.example.peloteros.model.Usuario> usuarioOpt = usuarioService.obtenerUsuarioPorEmail(email);

        if (!usuarioOpt.isPresent()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error de sesión: No se pudieron cargar los datos del usuario para ver las reservas.");
            return "redirect:/usuario/login?error=session_user_lookup_failed";
        }
        com.example.peloteros.model.Usuario usuario = usuarioOpt.get();

        try {
            List<Reserva> reservasActivas = new ArrayList<>();
            List<Reserva> reservasPendientes = reservaService.obtenerReservasPorUsuarioYEstado(usuario, "PENDIENTE");
            if (reservasPendientes != null) {
                reservasActivas.addAll(reservasPendientes);
            }

            List<Reserva> reservasConfirmadas = reservaService.obtenerReservasPorUsuarioYEstado(usuario, "CONFIRMADA");
            if (reservasConfirmadas != null) {
                reservasActivas.addAll(reservasConfirmadas);
            }

            List<Reserva> reservasPasadas = reservaService.obtenerReservasPasadas(usuario);
            if (reservasPasadas == null) {
                reservasPasadas = new ArrayList<>();
            }

            List<Reserva> reservasCanceladas = reservaService.obtenerReservasPorUsuarioYEstado(usuario, "CANCELADA");
            if (reservasCanceladas == null) {
                reservasCanceladas = new ArrayList<>();
            }

            Comparator<Reserva> porFecha = Comparator.comparing(Reserva::getFechaHoraInicio);
            reservasActivas.sort(porFecha);
            reservasPasadas.sort(porFecha.reversed());
            reservasCanceladas.sort(porFecha.reversed());

            model.addAttribute("reservasActivas", reservasActivas);
            model.addAttribute("reservasPasadas", reservasPasadas);
            model.addAttribute("reservasCanceladas", reservasCanceladas);
            model.addAttribute("usuario", usuario);

            return "misreservas";
        } catch (Exception e) {
            // Log the exception e
            System.err.println("Error al cargar mis reservas: " + e.getMessage()); // Basic logging
            redirectAttributes.addFlashAttribute("errorMessage", "Error al cargar las reservas: " + e.getMessage());
            return "redirect:/"; // Redirect to home on general error
        }
    }

    @PostMapping("/cancelar-reserva")
    public String cancelarReserva(
            @RequestParam Long reservaId,
            @RequestParam(required = false) String motivo,
            Principal principal,
            RedirectAttributes redirectAttributes) {

        if (principal == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Acceso denegado: No se pudo verificar la identidad del usuario.");
            return "redirect:/usuario/login"; 
        }
        String username = principal.getName();
        java.util.Optional<com.example.peloteros.model.Usuario> usuarioOpt = usuarioService.obtenerUsuarioPorEmail(username);

        if (!usuarioOpt.isPresent()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error: Usuario no encontrado en la base de datos.");
            // Redirect to misreservas as the original logic did, or perhaps to login if user context is critical for this page
            return "redirect:/reserva/misreservas"; 
        }
        // com.example.peloteros.model.Usuario usuario = usuarioOpt.get(); // Not strictly needed for service call as is

        try {
            // The service reservaService.cancelarReserva might need to be updated
            // to verify that the user making the request owns the reservation.
            // For now, it only takes reservaId and motivo.
            reservaService.cancelarReserva(reservaId, motivo);
            redirectAttributes.addFlashAttribute("successMessage", "La reserva ha sido cancelada exitosamente.");
        } catch (Exception e) {
            System.err.println("Error al cancelar reserva: " + e.getMessage()); // Basic logging
            redirectAttributes.addFlashAttribute("errorMessage", "No se pudo cancelar la reserva: " + e.getMessage());
        }

        return "redirect:/reserva/misreservas"; // Corrected redirect path
    }

    @GetMapping("/detalle/{id}") // Corrected mapping
    @ResponseBody
    public ResponseEntity<Reserva> getReservaDetalle(
            @PathVariable Long id,
            HttpSession session) {

        Usuario usuario = (Usuario) session.getAttribute("usuario");
        if (usuario == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            Reserva reserva = reservaService.obtenerReservaPorId(id);
            // Verificar que la reserva pertenece al usuario
            if (reserva != null && reserva.getUsuario().getId().equals(usuario.getId())) {
                return ResponseEntity.ok(reserva);
            }
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }
    @GetMapping("/confirmacion")
public String verConfirmacion(@RequestParam Long reservaId, Model model, Principal principal) {
    if (principal == null) {
        return "redirect:/usuario/login";
    }

    Reserva reserva = reservaService.obtenerReservaPorId(reservaId);
    if (reserva == null) {
        return "redirect:/reserva/misreservas";
    }

    model.addAttribute("reserva", reserva);
    return "confirmacion"; // Renderiza confirmacion.html
}


}
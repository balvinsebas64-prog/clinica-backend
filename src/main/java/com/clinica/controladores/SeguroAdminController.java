package com.clinica.controladores;

import com.clinica.entidades.*;
import com.clinica.repositorios.*;
import com.clinica.servicios.AuditoriaService;
import com.clinica.servicios.NotificacionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

@Controller
public class SeguroAdminController {

    @Autowired
    private SeguroRepository seguroRepo;

    @Autowired
    private PacienteRepository pacienteRepo;

    @Autowired
    private PagoSeguroRepository pagoRepo;

    @Autowired
    private CitaRepository citaRepo;

    @Autowired
    private NotificacionService notificacionService;

    @Autowired
    private UsuarioRepository usuarioRepo;

    // ⭐ AGREGADO AUDITORÍA
    @Autowired
    private AuditoriaService auditoriaService;

    // ==========================================
    // PERSONAL: VERIFICAR SEGURO
    // ==========================================
    @GetMapping("/personal/verificar")
    public String verificarSeguro(@RequestParam(required = false) String dni, Model model) {
        if (dni != null && !dni.isEmpty()) {
            Paciente paciente = pacienteRepo.findAll().stream()
                    .filter(p -> p.getDni().equals(dni))
                    .findFirst()
                    .orElse(null);

            if (paciente != null) {
                Seguro seguro = seguroRepo.findByPacienteId(paciente.getId()).orElse(null);
                List<Cita> citasHoy = new ArrayList<>();
                List<Cita> citasPaciente = citaRepo.findByPacienteId(paciente.getId());
                for (Cita c : citasPaciente) {
                    if (c.getFechaHora() != null && c.getFechaHora().toLocalDate().equals(LocalDateTime.now().toLocalDate())) {
                        citasHoy.add(c);
                    }
                }
                model.addAttribute("paciente", paciente);
                model.addAttribute("seguro", seguro);
                model.addAttribute("citasHoy", citasHoy);
            } else {
                model.addAttribute("error", "No se encontró ningún paciente con ese DNI");
            }
        }
        return "personal/verificar";
    }

    // ==========================================
    // ADMIN: LISTAR SEGUROS
    // ==========================================
    @GetMapping("/admin/seguros")
    public String listarSeguros(Model model) {
        List<Seguro> seguros = seguroRepo.findAll();
        model.addAttribute("seguros", seguros);
        
        long totalMorosos = seguros.stream().filter(s -> s.getEstado() == EstadoSeguro.MOROSO).count();
        long totalActivos = seguros.stream().filter(s -> s.getEstado() == EstadoSeguro.ACTIVO).count();
        model.addAttribute("totalMorosos", totalMorosos);
        model.addAttribute("totalActivos", totalActivos);
        
        return "admin/seguros";
    }

    // ==========================================
    // ADMIN: REGISTRAR PAGO MANUAL
    // ==========================================
    @PostMapping("/admin/seguros/pagar/{id}")
    public String registrarPago(@PathVariable Integer id) {
        Seguro seguro = seguroRepo.findById(id).orElse(null);
        if (seguro != null) {
            PagoSeguro pago = new PagoSeguro();
            pago.setSeguro(seguro);
            pago.setMesPagado(YearMonth.now().toString());
            pago.setMonto(seguro.getCuotaMensual());
            pago.setFechaPago(LocalDateTime.now());
            pago.setEstado(EstadoPago.PAGADO);
            pagoRepo.save(pago);

            seguro.setEstado(EstadoSeguro.ACTIVO);
            seguroRepo.save(seguro);

            // 🔔 NOTIFICAR AL PACIENTE
            if (seguro.getPaciente() != null && seguro.getPaciente().getEmail() != null) {
                Usuario user = usuarioRepo.findByEmail(seguro.getPaciente().getEmail()).orElse(null);
                if (user != null) {
                    notificacionService.crear(user, "Pago Registrado", 
                        "El administrador ha registrado tu pago. Tu seguro está ACTIVO.", "/paciente");
                }
            }

            // ⭐ AUDITORÍA
            auditoriaService.registrar("Registrar Pago Seguro", "Se registró pago para " + seguro.getPaciente().getNombre() + " por S/ " + seguro.getCuotaMensual());

            return "redirect:/admin/seguros?exito=Pago registrado correctamente. Seguro ACTIVO.";
        }
        return "redirect:/admin/seguros?error=No se encontró el seguro";
    }

    // ==========================================
    // ADMIN: MARCAR COMO MOROSO
    // ==========================================
    @PostMapping("/admin/seguros/moroso/{id}")
    public String marcarMoroso(@PathVariable Integer id) {
        Seguro seguro = seguroRepo.findById(id).orElse(null);
        if (seguro != null) {
            seguro.setEstado(EstadoSeguro.MOROSO);
            seguroRepo.save(seguro);

            // 🔔 NOTIFICAR AL PACIENTE
            if (seguro.getPaciente() != null && seguro.getPaciente().getEmail() != null) {
                Usuario user = usuarioRepo.findByEmail(seguro.getPaciente().getEmail()).orElse(null);
                if (user != null) {
                    notificacionService.crear(user, "¡Atención! Seguro Moroso", 
                        "Tu seguro ha sido marcado como MOROSO. Regulariza tu pago para agendar citas.", "/paciente");
                }
            }

            // ⭐ AUDITORÍA
            auditoriaService.registrar("Marcar Moroso", "Se marcó como moroso a " + seguro.getPaciente().getNombre());

            return "redirect:/admin/seguros?exito=Seguro marcado como MOROSO.";
        }
        return "redirect:/admin/seguros?error=No se encontró el seguro";
    }
}
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
import java.util.List;

@Controller
@RequestMapping("/paciente")
public class SeguroPagoController {

    @Autowired
    private SeguroRepository seguroRepo;

    @Autowired
    private PagoSeguroRepository pagoRepo;

    @Autowired
    private NotificacionService notificacionService;

    @Autowired
    private UsuarioRepository usuarioRepo;

    // ⭐ AGREGADO AUDITORÍA
    @Autowired
    private AuditoriaService auditoriaService;

    // 1. Mostrar la pantalla de pago
    @GetMapping("/pagar-seguro/{id}")
    public String mostrarFormularioPago(@PathVariable Integer id, Model model) {
        Seguro seguro = seguroRepo.findById(id).orElse(null);
        if (seguro == null) {
            return "redirect:/paciente?error=Seguro no encontrado";
        }

        String mesActual = YearMonth.now().toString();
        boolean yaPago = !pagoRepo.findBySeguroIdAndMesPagado(seguro.getId(), mesActual).isEmpty();

        model.addAttribute("seguro", seguro);
        model.addAttribute("mesAPagar", mesActual);
        model.addAttribute("yaPago", yaPago);

        return "paciente/pagar-seguro";
    }

    // 2. Procesar el pago
    @PostMapping("/pagar-seguro/procesar")
    public String procesarPago(
            @RequestParam("seguroId") Integer seguroId,
            @RequestParam("mesPagado") String mesPagado,
            @RequestParam("monto") Double monto,
            @RequestParam("metodoPago") String metodoPago) {

        Seguro seguro = seguroRepo.findById(seguroId).orElse(null);
        if (seguro == null) {
            return "redirect:/paciente?error=Seguro no encontrado";
        }

        if (!pagoRepo.findBySeguroIdAndMesPagado(seguroId, mesPagado).isEmpty()) {
            return "redirect:/paciente/pagar-seguro/" + seguroId + "?error=Ya realizaste el pago para este mes.";
        }

        PagoSeguro pago = new PagoSeguro();
        pago.setSeguro(seguro);
        pago.setMesPagado(mesPagado);
        pago.setMonto(monto);
        pago.setFechaPago(LocalDateTime.now());
        pago.setEstado(EstadoPago.PAGADO);

        pagoRepo.save(pago);

        seguro.setEstado(EstadoSeguro.ACTIVO);
        seguroRepo.save(seguro);

        // 🔔 NOTIFICAR AL PACIENTE (Éxito)
        if (seguro.getPaciente() != null && seguro.getPaciente().getEmail() != null) {
            Usuario user = usuarioRepo.findByEmail(seguro.getPaciente().getEmail()).orElse(null);
            if (user != null) {
                notificacionService.crear(user, "Pago de Seguro Exitoso", 
                    "Tu pago de S/ " + monto + " para el mes " + mesPagado + " ha sido registrado. ¡Ya estás al día!", "/paciente");
            }
        }

        // ⭐ AUDITORÍA
        auditoriaService.registrar("Pago en Línea", "El paciente " + seguro.getPaciente().getNombre() + " pagó S/ " + monto + " del mes " + mesPagado);

        return "redirect:/paciente/voucher-seguro/" + pago.getId();
    }

    // 3. Mostrar el voucher
    @GetMapping("/voucher-seguro/{id}")
    public String mostrarVoucher(@PathVariable Integer id, Model model) {
        PagoSeguro pago = pagoRepo.findById(id).orElse(null);
        if (pago == null) {
            return "redirect:/paciente?error=Pago no encontrado";
        }
        model.addAttribute("pago", pago);
        return "paciente/voucher-seguro";
    }

    // 4. Historial de pagos
    @GetMapping("/historial-seguro/{id}")
    public String historialPagos(@PathVariable Integer id, Model model) {
        Seguro seguro = seguroRepo.findById(id).orElse(null);
        if (seguro == null) {
            return "redirect:/paciente";
        }
        List<PagoSeguro> pagos = pagoRepo.findBySeguroIdOrderByMesPagadoDesc(id);
        model.addAttribute("seguro", seguro);
        model.addAttribute("pagos", pagos);
        return "paciente/historial-seguro";
    }
}
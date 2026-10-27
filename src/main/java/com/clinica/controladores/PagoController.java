package com.clinica.controladores;

import com.clinica.entidades.Cita;
import com.clinica.entidades.Pago;
import com.clinica.repositorios.CitaRepository;
import com.clinica.repositorios.PagoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Random;

@Controller
@RequestMapping("/pagos")
public class PagoController {

    @Autowired
    private PagoRepository pagoRepo;

    @Autowired
    private CitaRepository citaRepo;

    // Mostrar formulario de pago
    @GetMapping("/pagar/{citaId}")
    public String mostrarPago(@PathVariable Integer citaId, Model model) {
        Cita cita = citaRepo.findById(citaId).orElse(null);
        if (cita == null) {
            return "redirect:/paciente?error=Cita no encontrada";
        }

        model.addAttribute("cita", cita);
        return "pagos/pagar";
    }

    // Procesar el pago
    @PostMapping("/procesar")
    public String procesarPago(@RequestParam("citaId") Integer citaId,
                                @RequestParam("metodoPago") String metodoPago) {

        Cita cita = citaRepo.findById(citaId).orElse(null);
        if (cita == null) {
            return "redirect:/paciente?error=Cita no encontrada";
        }

        // Crear el pago
        Pago pago = new Pago();
        pago.setCitaId(citaId);
        pago.setMonto(cita.getPrecio() != null ? cita.getPrecio() : 50.0);
        pago.setMetodoPago(metodoPago);
        pago.setEstado("PAGADO");
        pago.setFechaPago(LocalDateTime.now());
        pago.setCreatedAt(LocalDateTime.now());

        // Generar número de voucher automático
        String numeroVoucher = generarVoucher();
        pago.setNumeroVoucher(numeroVoucher);

        pagoRepo.save(pago);

        // Vincular el pago con la cita
        cita.setPagoId(pago.getId());
        cita.setEstado(com.clinica.entidades.EstadoCita.CONFIRMADA);
        citaRepo.save(cita);

        return "redirect:/pagos/voucher/" + pago.getId();
    }

    // Mostrar voucher
    @GetMapping("/voucher/{pagoId}")
    public String mostrarVoucher(@PathVariable Integer pagoId, Model model) {
        Pago pago = pagoRepo.findById(pagoId).orElse(null);
        if (pago == null) {
            return "redirect:/paciente?error=Pago no encontrado";
        }

        Cita cita = citaRepo.findById(pago.getCitaId()).orElse(null);

        model.addAttribute("pago", pago);
        model.addAttribute("cita", cita);
        return "pagos/voucher";
    }

    // Generar número de voucher
    private String generarVoucher() {
        String fecha = LocalDateTime.now().toString().replaceAll("[^0-9]", "").substring(0, 14);
        int random = new Random().nextInt(9000) + 1000;
        return "VCH-" + fecha + "-" + random;
    }
}
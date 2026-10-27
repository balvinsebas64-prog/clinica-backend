package com.clinica.controladores;

import com.clinica.entidades.*;
import com.clinica.repositorios.HorarioMedicoRepository;
import com.clinica.repositorios.MedicoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalTime;
import java.util.*;

@Controller
@RequestMapping("/medicos")
public class HorarioMedicoController {

    @Autowired
    private MedicoRepository medicoRepo;

    @Autowired
    private HorarioMedicoRepository horarioRepo;

    @GetMapping("/{id}/horarios")
    public String editarHorarios(@PathVariable Integer id, Model model) {
        Medico medico = medicoRepo.findById(id).orElse(null);
        if (medico == null) {
            return "redirect:/medicos?error=Médico no encontrado";
        }

        List<HorarioMedico> horarios = horarioRepo.findByMedicoIdOrderByDiaSemanaAsc(id);

        Map<String, HorarioMedico> mapaHorarios = new HashMap<>();
        for (HorarioMedico h : horarios) {
            mapaHorarios.put(h.getDiaSemana().name(), h);
        }

        model.addAttribute("medico", medico);
        model.addAttribute("horarios", mapaHorarios);
        model.addAttribute("dias", DiaSemana.values());

        return "horarios/editar";
    }

    @Transactional  // ⭐ ESTA ES LA SOLUCIÓN
    @PostMapping("/{id}/horarios/guardar")
    public String guardarHorarios(
            @PathVariable Integer id,
            @RequestParam Map<String, String> params) {

        Medico medico = medicoRepo.findById(id).orElse(null);
        if (medico == null) {
            return "redirect:/medicos?error=Médico no encontrado";
        }

        // Eliminar horarios actuales (ahora sí funciona con @Transactional)
        horarioRepo.deleteByMedicoId(id);

        // Crear nuevos horarios
        for (DiaSemana dia : DiaSemana.values()) {
            String key = dia.name().toLowerCase();
            String disponible = params.get(key + "_disponible");
            String horaInicio = params.get(key + "_inicio");
            String horaFin = params.get(key + "_fin");

            if (disponible != null && horaInicio != null && !horaInicio.isEmpty()
                && horaFin != null && !horaFin.isEmpty()) {

                try {
                    // ⭐ Asegurar formato "HH:mm:ss"
                    if (horaInicio.length() == 5) horaInicio = horaInicio + ":00";
                    if (horaFin.length() == 5) horaFin = horaFin + ":00";

                    HorarioMedico h = new HorarioMedico();
                    h.setMedico(medico);
                    h.setDiaSemana(dia);
                    h.setHoraInicio(LocalTime.parse(horaInicio));
                    h.setHoraFin(LocalTime.parse(horaFin));
                    h.setDisponible(true);
                    horarioRepo.save(h);
                } catch (Exception e) {
                    return "redirect:/medicos/" + id + "/horarios?error=Formato de hora inválido en " + dia.getEtiqueta();
                }
            }
        }

        return "redirect:/medicos/" + id + "/horarios?exito=Horarios actualizados correctamente";
    }
}
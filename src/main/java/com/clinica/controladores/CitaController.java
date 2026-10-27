package com.clinica.controladores;

import com.clinica.entidades.*;
import com.clinica.repositorios.*;
import com.clinica.servicios.AuditoriaService;
import com.clinica.servicios.NotificacionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;

@Controller
@RequestMapping("/citas")
public class CitaController {

    @Autowired
    private CitaRepository citaRepo;

    @Autowired
    private PacienteRepository pacienteRepo;

    @Autowired
    private MedicoRepository medicoRepo;

    @Autowired
    private EspecialidadRepository especialidadRepo;

    @Autowired
    private UsuarioRepository usuarioRepo;

    @Autowired
    private HistoriaClinicaRepository historiaRepo;

    @Autowired
    private SeguroRepository seguroRepo;

    @Autowired
    private NotificacionService notificacionService;

    @Autowired
    private AuditoriaService auditoriaService;

    @Autowired
    private HorarioMedicoRepository horarioMedicoRepo;

    // ==========================================
    // LISTAR TODAS LAS CITAS
    // ==========================================
    @GetMapping
    public String listar(Model model) {
        model.addAttribute("citas", citaRepo.findAll());
        return "citas/listar";
    }

    // ==========================================
    // MOSTRAR FORMULARIO DE NUEVA CITA (PACIENTE)
    // ==========================================
    @GetMapping("/programar")
    public String programar(Model model, Authentication auth) {
        String username = auth.getName();
        Usuario usuario = usuarioRepo.findByUsername(username).orElse(null);
        Set<String> roles = AuthorityUtils.authorityListToSet(auth.getAuthorities());

        boolean esPaciente = roles.contains("ROLE_PACIENTE");

        if (esPaciente && usuario != null) {
            Paciente pacienteLogueado = pacienteRepo.findByEmail(usuario.getEmail()).orElse(null);
            model.addAttribute("esPaciente", true);
            model.addAttribute("pacienteLogueado", pacienteLogueado);
        } else {
            model.addAttribute("esPaciente", false);
            model.addAttribute("pacientes", pacienteRepo.findAll());
        }

        model.addAttribute("medicos", medicoRepo.findAll());
        model.addAttribute("especialidades", especialidadRepo.findAll());
        return "citas/programar";
    }

    // ==========================================
    // ⭐ NUEVO: FORMULARIO AUTOMÁTICO PARA EL PERSONAL
    // ==========================================
    @GetMapping("/personal/programar")
    public String mostrarProgramarParaPersonal(
            @RequestParam(required = false) Integer citaId,
            @RequestParam(required = false) Integer pacienteId,
            @RequestParam(required = false) Integer medicoId,
            Model model) {

        model.addAttribute("pacientes", pacienteRepo.findAll());
        model.addAttribute("medicos", medicoRepo.findAll());
        model.addAttribute("especialidades", especialidadRepo.findAll());

        if (citaId != null) {
            Cita cita = citaRepo.findById(citaId).orElse(null);
            model.addAttribute("cita", cita);
            if (pacienteId != null) model.addAttribute("pacienteSeleccionado", pacienteRepo.findById(pacienteId).orElse(null));
            if (medicoId != null) model.addAttribute("medicoSeleccionado", medicoRepo.findById(medicoId).orElse(null));
        }

        return "personal/programar";
    }

    // ==========================================
    // GUARDAR CITA (CON REDIRECCIÓN INTELIGENTE)
    // ==========================================
    @PostMapping("/guardar")
    public String guardar(@RequestParam("pacienteId") Integer pacienteId,
                          @RequestParam("medicoId") Integer medicoId,
                          @RequestParam("fechaHora") String fechaHora,
                          @RequestParam(value = "motivo", required = false, defaultValue = "") String motivo,
                          Authentication auth) {

        Paciente paciente = pacienteRepo.findById(pacienteId).orElse(null);
        Medico medico = medicoRepo.findById(medicoId).orElse(null);

        if (paciente == null || medico == null) {
            return "redirect:/citas/programar?error=No se encontró el paciente o médico";
        }

        Seguro seguro = seguroRepo.findByPacienteId(pacienteId).orElse(null);
        if (seguro != null && seguro.getEstado() == EstadoSeguro.MOROSO) {
            return "redirect:/citas/programar?error=Tu seguro está MOROSO. Regulariza tus pagos para agendar.";
        }

        LocalDateTime fecha;
        try {
            fecha = LocalDateTime.parse(fechaHora);
        } catch (Exception e) {
            return "redirect:/citas/programar?error=Formato de fecha inválido";
        }

        boolean pacienteDuplicado = citaRepo.findAll().stream()
                .anyMatch(c -> c.getPaciente() != null
                            && c.getPaciente().getId().equals(pacienteId)
                            && c.getMedico() != null
                            && c.getMedico().getId().equals(medicoId)
                            && c.getFechaHora() != null
                            && c.getFechaHora().equals(fecha)
                            && c.getEstado() != EstadoCita.CANCELADA);

        if (pacienteDuplicado) {
            return "redirect:/citas/programar?error=Ya tienes una cita con este médico a la misma hora.";
        }

        boolean medicoOcupado = citaRepo.findAll().stream()
                .anyMatch(c -> c.getMedico() != null
                            && c.getMedico().getId().equals(medicoId)
                            && c.getFechaHora() != null
                            && c.getFechaHora().equals(fecha)
                            && c.getEstado() != EstadoCita.CANCELADA);

        if (medicoOcupado) {
            return "redirect:/citas/programar?error=El doctor ya tiene una cita programada en ese horario. Por favor, elige otra hora.";
        }

        DiaSemana diaCita = convertirDiaSemana(fecha.getDayOfWeek());
        Optional<HorarioMedico> horarioOpt = horarioMedicoRepo.findByMedicoIdAndDiaSemana(medicoId, diaCita);

        if (horarioOpt.isEmpty()) {
            return "redirect:/citas/programar?error=El doctor no atiende el dia " + diaCita.getEtiqueta() + ". Por favor, elige otro dia.";
        }

        HorarioMedico horario = horarioOpt.get();
        if (fecha.toLocalTime().isBefore(horario.getHoraInicio()) || fecha.toLocalTime().isAfter(horario.getHoraFin())) {
            return "redirect:/citas/programar?error=El doctor atiende solo de " 
                + horario.getHoraInicio() + " a " + horario.getHoraFin() + " los " + diaCita.getEtiqueta();
        }

        Cita cita = new Cita();
        cita.setPaciente(paciente);
        cita.setMedico(medico);
        cita.setFechaHora(fecha);
        cita.setMotivo(motivo);
        cita.setEstado(EstadoCita.PENDIENTE);
        cita.setCreatedAt(LocalDateTime.now());

        if (medico.getEspecialidad() != null) {
            cita.setPrecio(medico.getEspecialidad().getPrecio());
        } else {
            cita.setPrecio(50.0);
        }

        citaRepo.save(cita);

        if (medico.getUsuario() != null) {
            notificacionService.crear(
                medico.getUsuario(),
                "Nueva Cita Agendada",
                "El paciente " + paciente.getNombre() + " " + paciente.getApellido() + " agendó una cita para el " + fecha,
                "/doctor"
            );
        }

        auditoriaService.registrar("Agendar Cita", "El paciente " + paciente.getNombre() + " agendó cita con " + medico.getNombre());

        // ⭐ REDIRECCIÓN INTELIGENTE SEGÚN EL ROL
        Set<String> roles = AuthorityUtils.authorityListToSet(auth.getAuthorities());

        if (roles.contains("ROLE_PACIENTE")) {
            return "redirect:/paciente?exito=Cita programada correctamente";
        } else if (roles.contains("ROLE_PERSONAL_ADMINISTRATIVO")) {
            return "redirect:/personal?exito=Cita programada correctamente";
        } else {
            return "redirect:/admin?exito=Cita programada correctamente";
        }
    }

    // ==========================================
    // GUARDAR REPROGRAMACIÓN AUTOMÁTICA (PERSONAL)
    // ==========================================
    @PostMapping("/personal/programar/guardar")
    public String guardarProgramarPersonal(@RequestParam("citaId") Integer citaId,
                                           @RequestParam("pacienteId") Integer pacienteId,
                                           @RequestParam("medicoId") Integer medicoId,
                                           @RequestParam("fechaHora") String fechaHora) {
        Cita cita = citaRepo.findById(citaId).orElse(null);
        if (cita == null) return "redirect:/personal?error=Cita no encontrada";

        try {
            LocalDateTime nuevaFecha = LocalDateTime.parse(fechaHora);
            DiaSemana diaCita = convertirDiaSemana(nuevaFecha.getDayOfWeek());
            Optional<HorarioMedico> horarioOpt = horarioMedicoRepo.findByMedicoIdAndDiaSemana(medicoId, diaCita);

            if (horarioOpt.isEmpty()) {
                return "redirect:/citas/personal/programar?citaId=" + citaId + "&pacienteId=" + pacienteId + "&medicoId=" + medicoId + "&error=El doctor no atiende ese dia.";
            }

            HorarioMedico horario = horarioOpt.get();
            if (nuevaFecha.toLocalTime().isBefore(horario.getHoraInicio()) || nuevaFecha.toLocalTime().isAfter(horario.getHoraFin())) {
                return "redirect:/citas/personal/programar?citaId=" + citaId + "&pacienteId=" + pacienteId + "&medicoId=" + medicoId + "&error=El doctor atiende solo de " + horario.getHoraInicio() + " a " + horario.getHoraFin();
            }

            cita.setFechaHora(nuevaFecha);
            citaRepo.save(cita);
            
            if (cita.getPaciente() != null && cita.getPaciente().getEmail() != null) {
                Usuario userPaciente = usuarioRepo.findByEmail(cita.getPaciente().getEmail()).orElse(null);
                if (userPaciente != null) {
                    notificacionService.crear(userPaciente, "Cita Reprogramada", "Tu cita con el Dr. " + cita.getMedico().getNombre() + " ha sido cambiada para el " + nuevaFecha, "/paciente");
                }
            }

            auditoriaService.registrar("Reprogramar Cita (Automático)", "Se reprogramó la cita ID " + citaId + " para el " + fechaHora);
            return "redirect:/personal?exito=Cita reprogramada correctamente";
            
        } catch (Exception e) {
            return "redirect:/citas/personal/programar?citaId=" + citaId + "&pacienteId=" + pacienteId + "&medicoId=" + medicoId + "&error=Error al guardar.";
        }
    }

    private DiaSemana convertirDiaSemana(DayOfWeek dayOfWeek) {
        return switch (dayOfWeek) {
            case MONDAY -> DiaSemana.LUNES;
            case TUESDAY -> DiaSemana.MARTES;
            case WEDNESDAY -> DiaSemana.MIERCOLES;
            case THURSDAY -> DiaSemana.JUEVES;
            case FRIDAY -> DiaSemana.VIERNES;
            case SATURDAY -> DiaSemana.SABADO;
            case SUNDAY -> DiaSemana.DOMINGO;
        };
    }

    // ==========================================
    // ⭐ NUEVO: MÉDICO SOLICITA REPROGRAMACIÓN
    // ==========================================
    @GetMapping("/solicitar-reprogramacion/{id}")
    public String solicitarReprogramacion(@PathVariable Integer id) {
        Cita cita = citaRepo.findById(id).orElse(null);
        if (cita == null) {
            return "redirect:/doctor?error=Cita no encontrada";
        }

        for (Usuario u : usuarioRepo.findAll()) {
            boolean esPersonal = u.getRoles().stream()
                .anyMatch(r -> r.getNombre().equals("PERSONAL_ADMINISTRATIVO"));
            
            if (esPersonal) {
                notificacionService.crear(
                    u,
                    "Solicitud de Reprogramación",
                    "El Dr. " + cita.getMedico().getNombre() + " solicita reprogramar la cita ID " + cita.getId() 
                        + " del paciente " + cita.getPaciente().getNombre() + " " + cita.getPaciente().getApellido() + ".",
                    "/citas/personal/programar?citaId=" + cita.getId() + "&pacienteId=" + cita.getPaciente().getId() + "&medicoId=" + cita.getMedico().getId()
                );
            }
        }

        auditoriaService.registrar("Solicitar Reprogramación", "El médico solicitó cambiar la cita ID " + id);
        return "redirect:/doctor?exito=Solicitud enviada al personal administrativo";
    }

    // ==========================================
    // LISTAR CITAS PARA CANCELAR
    // ==========================================
    @GetMapping("/cancelar")
    public String listarParaCancelar(Model model) {
        model.addAttribute("citas", citaRepo.findAll());
        return "citas/cancelar";
    }

    @GetMapping("/cancelar/{id}")
    public String cancelarCita(@PathVariable Integer id) {
        Cita cita = citaRepo.findById(id).orElse(null);
        if (cita != null) {
            cita.setEstado(EstadoCita.CANCELADA);
            citaRepo.save(cita);
            auditoriaService.registrar("Cancelar Cita", "Se canceló la cita ID " + id + " del paciente " + cita.getPaciente().getNombre());
        }
        return "redirect:/citas/cancelar?exito=Cita cancelada correctamente";
    }

    // ==========================================
    // DIAGNÓSTICO
    // ==========================================
    @GetMapping("/diagnostico/{id}")
    public String mostrarDiagnostico(@PathVariable Integer id, Model model) {
        Cita cita = citaRepo.findById(id).orElse(null);
        if (cita == null) {
            return "redirect:/doctor?error=Cita no encontrada";
        }

        model.addAttribute("cita", cita);
        HistoriaClinica historia = historiaRepo.findByPacienteId(cita.getPaciente().getId()).orElse(null);
        model.addAttribute("historia", historia);
        return "citas/diagnostico";
    }

    @PostMapping("/diagnostico/guardar")
    public String guardarDiagnostico(@RequestParam("citaId") Integer citaId,
                                     @RequestParam("diagnostico") String diagnostico,
                                     @RequestParam("medicamentos") String medicamentos,
                                     @RequestParam("condicionesMedicas") String condicionesMedicas) {

        Cita cita = citaRepo.findById(citaId).orElse(null);
        if (cita == null) {
            return "redirect:/doctor?error=Cita no encontrada";
        }

        HistoriaClinica historia = historiaRepo.findByPacienteId(cita.getPaciente().getId()).orElse(null);
        if (historia == null) {
            historia = new HistoriaClinica();
            historia.setPaciente(cita.getPaciente());
            historia.setFechaCreacion(LocalDateTime.now());
        }

        historia.setDiagnostico(diagnostico);
        historia.setMedicamentos(medicamentos);
        historia.setCondicionesMedicas(condicionesMedicas);
        historiaRepo.save(historia);

        cita.setEstado(EstadoCita.ATENDIDA);
        citaRepo.save(cita);

        auditoriaService.registrar("Guardar Diagnóstico", "Se atendió al paciente " + cita.getPaciente().getNombre() + " en la cita " + citaId);

        return "redirect:/doctor?exito=Diagnóstico guardado correctamente";
    }

    // ==========================================
    // REPROGRAMAR CITA (MANUAL)
    // ==========================================
    @GetMapping("/reprogramar")
    public String listarParaReprogramar(Model model) {
        model.addAttribute("citas", citaRepo.findAll());
        return "citas/reprogramar";
    }

    @GetMapping("/reprogramar/{id}")
    public String mostrarReprogramar(@PathVariable Integer id, Model model) {
        Cita cita = citaRepo.findById(id).orElse(null);
        if (cita == null) {
            return "redirect:/citas/reprogramar?error=Cita no encontrada";
        }
        model.addAttribute("cita", cita);
        return "citas/reprogramar-form";
    }

    @PostMapping("/reprogramar/guardar")
    public String guardarReprogramar(@RequestParam("citaId") Integer citaId,
                                     @RequestParam("fechaHora") String fechaHora) {
        Cita cita = citaRepo.findById(citaId).orElse(null);
        if (cita != null) {
            try {
                LocalDateTime nuevaFecha = LocalDateTime.parse(fechaHora);

                boolean medicoOcupado = citaRepo.findAll().stream()
                        .anyMatch(c -> c.getMedico() != null
                                    && c.getMedico().getId().equals(cita.getMedico().getId())
                                    && c.getFechaHora() != null
                                    && c.getFechaHora().equals(nuevaFecha)
                                    && c.getEstado() != EstadoCita.CANCELADA
                                    && !c.getId().equals(citaId));

                if (medicoOcupado) {
                    return "redirect:/citas/reprogramar?error=El doctor ya tiene otra cita en ese horario.";
                }

                DiaSemana diaCita = convertirDiaSemana(nuevaFecha.getDayOfWeek());
                Optional<HorarioMedico> horarioOpt = horarioMedicoRepo.findByMedicoIdAndDiaSemana(cita.getMedico().getId(), diaCita);

                if (horarioOpt.isEmpty()) {
                    return "redirect:/citas/reprogramar?error=El doctor no atiende el dia " + diaCita.getEtiqueta();
                }

                HorarioMedico horario = horarioOpt.get();
                if (nuevaFecha.toLocalTime().isBefore(horario.getHoraInicio()) || nuevaFecha.toLocalTime().isAfter(horario.getHoraFin())) {
                    return "redirect:/citas/reprogramar?error=El doctor atiende solo de " + horario.getHoraInicio() + " a " + horario.getHoraFin();
                }

                cita.setFechaHora(nuevaFecha);
                citaRepo.save(cita);

                if (cita.getPaciente() != null && cita.getPaciente().getEmail() != null) {
                    Usuario userPaciente = usuarioRepo.findByEmail(cita.getPaciente().getEmail()).orElse(null);
                    if (userPaciente != null) {
                        notificacionService.crear(
                            userPaciente,
                            "Cita Reprogramada",
                            "Tu cita con el Dr. " + cita.getMedico().getNombre() + " ha sido reprogramada para el " + nuevaFecha,
                            "/paciente"
                        );
                    }
                }

                auditoriaService.registrar("Reprogramar Cita", "Se reprogramó la cita ID " + citaId + " para el " + fechaHora);
                return "redirect:/citas/reprogramar?exito=Cita reprogramada correctamente";
            } catch (Exception e) {
                return "redirect:/citas/reprogramar?error=Formato de fecha inválido";
            }
        }
        return "redirect:/citas/reprogramar?error=Cita no encontrada";
    }
}
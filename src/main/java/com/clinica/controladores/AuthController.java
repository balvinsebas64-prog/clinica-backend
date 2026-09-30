package com.clinica.controladores;

import com.clinica.entidades.Paciente;
import com.clinica.entidades.Rol;
import com.clinica.entidades.Usuario;
import com.clinica.repositorios.PacienteRepository;
import com.clinica.repositorios.RolRepository;
import com.clinica.repositorios.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@Controller
public class AuthController {

    @Autowired
    private UsuarioRepository usuarioRepo;

    @Autowired
    private PacienteRepository pacienteRepo;

    @Autowired
    private RolRepository rolRepo;

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/registro")
    public String registroForm() {
        return "registro";
    }

    @PostMapping("/registro/guardar")
    public String registroGuardar(
            @RequestParam String nombre,
            @RequestParam String apellido,
            @RequestParam String dni,
            @RequestParam(required = false) String telefono,
            @RequestParam String email,
            @RequestParam(required = false) String fechaNacimiento,
            @RequestParam(required = false) String direccion,
            @RequestParam String username,
            @RequestParam String password,
            Model model) {

        // Verificar si ya existe el usuario
        if (usuarioRepo.findByUsername(username).isPresent()) {
            model.addAttribute("error", "El nombre de usuario ya existe");
            return "registro";
        }

        // Verificar si ya existe el DNI
        if (pacienteRepo.findByDni(dni).isPresent()) {
            model.addAttribute("error", "El DNI ya está registrado");
            return "registro";
        }

        try {
            // 1. Crear el Usuario
            Usuario usuario = new Usuario();
            usuario.setUsername(username);
            usuario.setPassword(password);
            usuario.setEmail(email);
            usuario.setNombreCompleto(nombre + " " + apellido);
            usuario.setEnabled(true);

            // Asignar rol PACIENTE
            Rol rolPaciente = rolRepo.findAll().stream()
                .filter(r -> r.getNombre().equals("PACIENTE"))
                .findFirst()
                .orElse(null);

            if (rolPaciente != null) {
                Set<Rol> roles = new HashSet<>();
                roles.add(rolPaciente);
                usuario.setRoles(roles);
            }

            usuarioRepo.save(usuario);

            // 2. Crear el Paciente
            Paciente paciente = new Paciente();
            paciente.setNombre(nombre);
            paciente.setApellido(apellido);
            paciente.setDni(dni);
            paciente.setTelefono(telefono);
            paciente.setEmail(email);

            if (fechaNacimiento != null && !fechaNacimiento.isEmpty()) {
                paciente.setFechaNacimiento(LocalDate.parse(fechaNacimiento));
            }

            paciente.setDireccion(direccion);

            pacienteRepo.save(paciente);

            // 3. Redirigir al login con mensaje de éxito
            return "redirect:/login?registroExitoso";

        } catch (Exception e) {
            model.addAttribute("error", "Error al registrar: " + e.getMessage());
            return "registro";
        }
    }
}
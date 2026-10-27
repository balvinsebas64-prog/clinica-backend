package com.clinica.controladores;

import com.clinica.entidades.Medico;
import com.clinica.entidades.Rol;
import com.clinica.entidades.Usuario;
import com.clinica.repositorios.MedicoRepository;
import com.clinica.repositorios.RolRepository;
import com.clinica.repositorios.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Controller
@RequestMapping("/usuarios")
public class UsuarioController {

    @Autowired
    private UsuarioRepository usuarioRepo;

    @Autowired
    private RolRepository rolRepo;

    @Autowired
    private MedicoRepository medicoRepo;

    // ⭐ LISTAR CON BUSCADOR
    @GetMapping
    public String listar(
            @RequestParam(value = "buscar", required = false) String buscar,
            @RequestParam(value = "rolId", required = false) Integer rolId,
            Model model) {

        List<Usuario> usuarios = usuarioRepo.findAll(); // Empezamos con todos

        // 1. Filtrar por Rol (si se seleccionó uno)
        if (rolId != null) {
            usuarios = usuarioRepo.findByRoles_Id(rolId);
            model.addAttribute("rolSeleccionado", rolId);
        }

        // 2. Filtrar por Texto (si se escribió algo)
        if (buscar != null && !buscar.isEmpty()) {
            usuarios = usuarios.stream()
                .filter(u -> u.getUsername().toLowerCase().contains(buscar.toLowerCase()) ||
                             (u.getNombreCompleto() != null && u.getNombreCompleto().toLowerCase().contains(buscar.toLowerCase())) ||
                             (u.getEmail() != null && u.getEmail().toLowerCase().contains(buscar.toLowerCase())))
                .collect(java.util.stream.Collectors.toList());
            model.addAttribute("buscar", buscar);
        }

        model.addAttribute("usuarios", usuarios);
        model.addAttribute("roles", rolRepo.findAll()); // Para llenar el combo de roles
        return "usuario/listar";
    }

    @GetMapping("/crear")
    public String crear(Model model) {
        model.addAttribute("usuario", new Usuario());
        model.addAttribute("roles", rolRepo.findAll());
        model.addAttribute("medicos", medicoRepo.findByUsuarioIsNull());
        model.addAttribute("modoEdicion", false);
        return "usuario/crear";
    }

    @GetMapping("/editar/{id}")
    public String editar(@PathVariable Integer id, Model model) {
        Usuario usuario = usuarioRepo.findById(id)
            .orElseThrow(() -> new RuntimeException("Usuario no encontrado: " + id));
        model.addAttribute("usuario", usuario);
        model.addAttribute("roles", rolRepo.findAll());
        model.addAttribute("medicos", medicoRepo.findByUsuarioIsNull());
        model.addAttribute("modoEdicion", true);
        return "usuario/crear";
    }

    @PostMapping("/guardar")
    public String guardar(@ModelAttribute Usuario usuario,
                          @RequestParam(value = "rolId", required = false) Integer rolId,
                          @RequestParam(value = "medicoId", required = false) Integer medicoId) {
        boolean esNuevo = (usuario.getId() == null);

        if (rolId != null) {
            Rol rol = rolRepo.findById(rolId).orElse(null);
            if (rol != null) {
                Set<Rol> roles = new HashSet<>();
                roles.add(rol);
                usuario.setRoles(roles);
            }
        }

        usuario = usuarioRepo.save(usuario);

        if (medicoId != null) {
            Medico medico = medicoRepo.findById(medicoId).orElse(null);
            if (medico != null) {
                medico.setUsuario(usuario);
                medicoRepo.save(medico);
            }
        }

        String mensaje = esNuevo ? "Usuario creado correctamente" : "Usuario actualizado correctamente";
        return "redirect:/usuarios?exito=" + mensaje;
    }

    @GetMapping("/eliminar/{id}")
    public String eliminar(@PathVariable Integer id) {
        Medico medico = medicoRepo.findAll().stream()
            .filter(m -> m.getUsuario() != null && m.getUsuario().getId().equals(id))
            .findFirst()
            .orElse(null);
        if (medico != null) {
            medico.setUsuario(null);
            medicoRepo.save(medico);
        }

        usuarioRepo.deleteById(id);
        return "redirect:/usuarios?exito=Usuario eliminado correctamente";
    }
}
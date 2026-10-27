package com.clinica.controladores;

import com.clinica.entidades.Configuracion;
import com.clinica.servicios.ConfiguracionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/admin/configuracion")
public class ConfiguracionController {

    @Autowired
    private ConfiguracionService configuracionService;

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("configuraciones", configuracionService.listarTodas());
        return "admin/configuracion";
    }

    @GetMapping("/editar/{id}")
    public String editar(@PathVariable Integer id, Model model) {
        Configuracion config = configuracionService.obtenerPorId(id);
        if (config == null) {
            return "redirect:/admin/configuracion";
        }
        model.addAttribute("configuracion", config);
        return "admin/configuracion-editar";
    }

    @PostMapping("/guardar")
    public String guardar(@ModelAttribute Configuracion config) {
        configuracionService.guardar(config);
        return "redirect:/admin/configuracion?exito=Configuración actualizada";
    }
}
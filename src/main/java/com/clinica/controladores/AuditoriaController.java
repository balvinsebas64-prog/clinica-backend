package com.clinica.controladores;

import com.clinica.servicios.AuditoriaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AuditoriaController {

    @Autowired
    private AuditoriaService auditoriaService;

    @GetMapping("/admin/auditoria")
    public String verAuditoria(Model model) {
        model.addAttribute("registros", auditoriaService.listarTodo());
        return "admin/auditoria";
    }
}
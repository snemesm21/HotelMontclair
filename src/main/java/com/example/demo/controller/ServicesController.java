package com.example.demo.controller;

import com.example.demo.entities.Service;
import com.example.demo.service.ServiceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/services")
public class ServicesController {

    @Autowired
    private ServiceService serviceService;

    // http://localhost:8080/services
    @GetMapping
    public String index(Model model) {
        model.addAttribute("services", serviceService.searchAll());
        return "services";
    }

    // http://localhost:8080/services/table
    @GetMapping("/table")
    public String tableView(Model model) {
        model.addAttribute("services", serviceService.searchAll());
        return "services-table";
    }

    // http://localhost:8080/services/1
    @GetMapping("/{id}")
    public String detail(@PathVariable("id") Long id, Model model) {
        Service svc = serviceService.searchById(id);
        if (svc != null) {
            model.addAttribute("service", svc);
            return "service-detail";
        }
        return "redirect:/services";
    }

    // http://localhost:8080/services/add
    @GetMapping("/add")
    public String mostrarFormularioCrear(Model model) {
        Service service = new Service();
        service.setId(null);
        model.addAttribute("service", service);
        model.addAttribute("pageTitle", "Nuevo Servicio");
        return "service-form";
    }

    // http://localhost:8080/services/add (POST)
    @PostMapping(value = {"/add", "/save"})
    public String agregarServicio(@ModelAttribute("service") Service service) {
        serviceService.save(service);
        return "redirect:/services";
    }

    // http://localhost:8080/services/update/1
    @GetMapping("/update/{id}")
    public String mostrarFormularioEditar(@PathVariable("id") Long id, Model model) {
        Service service = serviceService.searchById(id);
        model.addAttribute("service", service);
        model.addAttribute("pageTitle", "Modificar Servicio");
        return "service-form";
    }

    // http://localhost:8080/services/delete/1
    @GetMapping("/delete/{id}")
    public String eliminarServicio(@PathVariable("id") Long id) {
        serviceService.delete(id);
        return "redirect:/services";
    }
}
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
        try {
            model.addAttribute("services", serviceService.searchAll().stream()
                    .filter(s -> !s.isHidden())
                    .toList());
            return "services";
        } catch (Exception e) {
            model.addAttribute("mensaje", "Error al cargar los servicios: " + e.getMessage());
            return "error";
        }
    }

    @GetMapping("/toggle-visibility/{id}")
    public String toggleVisibility(@PathVariable("id") Long id, Model model) {
        try {
            Service service = serviceService.searchById(id);
            if (service != null) {
                service.setHidden(!service.isHidden());
                serviceService.save(service);
            }
            return "redirect:/services/table";
        } catch (com.example.demo.errors.NotFoundException e) {
            throw e;
        } catch (Exception e) {
            model.addAttribute("mensaje", "Error al cambiar visibilidad: " + e.getMessage());
            return "error";
        }
    }

    // http://localhost:8080/services/table
    @GetMapping("/table")
    public String tableView(Model model) {
        try {
            model.addAttribute("services", serviceService.searchAll());
            return "services-table";
        } catch (Exception e) {
            model.addAttribute("mensaje", "Error al cargar la tabla de servicios: " + e.getMessage());
            return "error";
        }
    }

    // http://localhost:8080/services/1
    @GetMapping("/{id}")
    public String detail(@PathVariable("id") Long id, Model model) {
        try {
            Service svc = serviceService.searchById(id);
            model.addAttribute("service", svc);
            return "service-detail";
        } catch (com.example.demo.errors.NotFoundException e) {
            throw e;
        } catch (Exception e) {
            model.addAttribute("mensaje", "Error al cargar el detalle del servicio: " + e.getMessage());
            return "error";
        }
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
    public String agregarServicio(@ModelAttribute("service") Service service, Model model) {
        try {
            serviceService.save(service);
            return "redirect:/services";
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            model.addAttribute("mensaje", "No se pudo guardar: Ya existe un servicio con la etiqueta (tag) '" + service.getTag() + "'.");
            return "error";
        } catch (Exception e) {
            model.addAttribute("mensaje", "Error al guardar el servicio: " + e.getMessage());
            return "error";
        }
    }

    // http://localhost:8080/services/update/1
    @GetMapping("/update/{id}")
    public String mostrarFormularioEditar(@PathVariable("id") Long id, Model model) {
        try {
            Service service = serviceService.searchById(id);
            model.addAttribute("service", service);
            model.addAttribute("pageTitle", "Modificar Servicio");
            return "service-form";
        } catch (com.example.demo.errors.NotFoundException e) {
            throw e;
        } catch (Exception e) {
            model.addAttribute("mensaje", "Error al cargar el formulario de edición: " + e.getMessage());
            return "error";
        }
    }

    // http://localhost:8080/services/delete/1
    @GetMapping("/delete/{id}")
    public String eliminarServicio(@PathVariable("id") Long id, Model model) {
        try {
            serviceService.delete(id);
            return "redirect:/services";
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            model.addAttribute("mensaje", "No se puede eliminar el servicio porque está siendo utilizado en otros registros (por ejemplo, reservaciones).");
            return "error";
        } catch (com.example.demo.errors.NotFoundException e) {
            throw e;
        } catch (Exception e) {
            model.addAttribute("mensaje", "Error al eliminar el servicio: " + e.getMessage());
            return "error";
        }
    }
}
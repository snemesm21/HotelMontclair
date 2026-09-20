package com.example.demo.controller;

import com.example.demo.entities.Client;
import com.example.demo.service.ClientService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/profile")
public class ProfileController {

    @Autowired
    private ClientService clientService;

    @GetMapping
    public String profileWithoutId() {
        return "redirect:/login";
    }

    @GetMapping("/{id}")
    public String viewProfile(@PathVariable Long id, Model model) {
        try {
            Client client = clientService.findById(id);
            model.addAttribute("client", client);
            return "profile";
        } catch (com.example.demo.errors.NotFoundException e) {
            throw e;
        } catch (Exception e) {
            model.addAttribute("mensaje", "Error inesperado al cargar el perfil: " + e.getMessage());
            return "error";
        }
    }

    @PostMapping("/edit/{id}")
    public String editProfile(@PathVariable Long id, @ModelAttribute Client client, Model model) {
        try {
            client.setId(id);
            clientService.save(client);
            return "redirect:/profile/" + id + "?updated=true";
        } catch (Exception e) {
            model.addAttribute("mensaje", "Error inesperado al guardar el perfil: " + e.getMessage());
            return "error";
        }
    }

    @PostMapping("/delete/{id}")
    public String deleteProfilePost(@PathVariable Long id, Model model) {
        try {
            clientService.delete(id);
            return "redirect:/login?deleted=true";
        } catch (com.example.demo.errors.NotFoundException e) {
            throw e;
        } catch (Exception e) {
            model.addAttribute("mensaje", "Error inesperado al eliminar el perfil: " + e.getMessage());
            return "error";
        }
    }

    @GetMapping("/delete/{id}")
    public String deleteProfileGet(@PathVariable Long id, Model model) {
        try {
            clientService.delete(id);
            return "redirect:/login?deleted=true";
        } catch (com.example.demo.errors.NotFoundException e) {
            throw e;
        } catch (Exception e) {
            model.addAttribute("mensaje", "Error inesperado al eliminar el perfil: " + e.getMessage());
            return "error";
        }
    }
}

package com.example.demo.controller;

import com.example.demo.entities.Client;
import com.example.demo.service.ClientService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@Controller
@RequestMapping("/clients")
public class ClientController {
    private final ClientService service;

    public ClientController(ClientService service) {
        this.service = service;
    }

    @GetMapping
    public String list(Model model) {
        List<Client> list = service.findAll();
        model.addAttribute("clients", list);
        return "clients"; // thymeleaf template
    }

    @GetMapping("/add")
    public String addForm(Model model) {
        model.addAttribute("client", new Client());
        return "client-form";
    }

    @PostMapping("/add")
    public String add(@ModelAttribute Client client) {
        service.save(client);
        return "redirect:/clients";
    }

    @GetMapping("/edit/{id}")
    public String editForm(@PathVariable Long id, Model model) {
        Client c = service.findById(id);
        model.addAttribute("client", c);
        return "client-form";
    }

    @PostMapping("/edit/{id}")
    public String edit(@PathVariable Long id, @ModelAttribute Client client) {
        client.setId(id);
        service.save(client);
        return "redirect:/clients";
    }

    @GetMapping("/delete/{id}")
    public String delete(@PathVariable Long id) {
        service.delete(id);
        return "redirect:/clients";
    }
}


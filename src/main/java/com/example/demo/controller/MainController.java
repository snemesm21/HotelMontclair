package com.example.demo.controller;

import com.example.demo.service.ServiceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class MainController {

    @Autowired
    private ServiceService serviceService;

    @GetMapping({"/", "/index", "/home"})
    public String index(Model model) {
        model.addAttribute("services", serviceService.searchAll());
        return "index";
    }
}
package com.example.demo.errors;

import org.springframework.ui.Model;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public String handleInvalidParameter(MethodArgumentTypeMismatchException exception, Model model) {
        model.addAttribute("mensaje", "El identificador recibido no tiene un formato válido.");
        return "error";
    }

    @ExceptionHandler(Exception.class)
    public String handleError(Exception exception, Model model) {
        model.addAttribute("mensaje", exception.getMessage() != null
                ? exception.getMessage() : "Ocurrió un error inesperado.");
        return "error";
    }
}
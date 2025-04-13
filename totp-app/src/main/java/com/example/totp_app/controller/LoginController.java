package com.example.totp_app.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
// Клас-контролер, що відповідає за показ сторінки входу
public class LoginController {
    // Обробка GET-запиту за адресою /login
    @GetMapping("/login")
    public String loginPage() {
        return "login"; // завантаження login.html
    }
}
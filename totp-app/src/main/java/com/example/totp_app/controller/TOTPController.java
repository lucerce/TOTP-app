package com.example.totp_app.controller;

import com.example.totp_app.model.TOTPForm;
import com.example.totp_app.service.TOTPService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpSession;

@Controller
// Клас-контролер, що відповідає за обробку веб-запитів
public class TOTPController {

    private final int digits = 6;          // кількість цифр у TOTP-коді
    private final int timeStep = 30;       // час, протягом якого код дійсний (сек)
    private final TOTPService totpService; // створює TOTP-коди (з класу TOTPService)

    // Передача TOTPService до TOTPController
    public TOTPController(TOTPService totpService) {
        this.totpService = totpService;
    }
    // Сторінка з полем для введення ТОТР
    @GetMapping("/totp")
    public String totpPage(Model model) {
        long currentTime = System.currentTimeMillis();
        // Генерація поточного TOTP-коду (цей код не передається на сайт)
        String totp = totpService.generateTOTP(currentTime, digits, timeStep);
        // Об'єкт форми, щоб користувач міг ввести пароль
        model.addAttribute("totpForm", new TOTPForm());
        return "totp";
    }

    // Обробка натискання кнопки "Перевірити" після введення коду
    @PostMapping("/totp-verify")
    public String verifyTotp(@ModelAttribute TOTPForm totpForm, HttpSession session) {
        String currentTotp = totpService.getCurrentTotp(); // поточний TOTP
        // Перевірка, чи збігається введений користувачем пароль із поточним
        if (totpForm.getCode().equals(currentTotp)) {
            // Аутентифікація пройшла успішно
            session.setAttribute("TOTP_AUTHENTICATED", true);
            return "redirect:/home"; // наступна сторінка
        } else {
            // Код неправильний або прострочений
            return "redirect:/totp?error=true";
        }
    }

    // Головна сторінка, яка відкривається тільки якщо користувач пройшов перевірку TOTP
    @GetMapping("/home")
    public String home(HttpSession session) {
        // Перевірка, чи є у сесії відмітка про успішне проходження TOTP
        if (session.getAttribute("TOTP_AUTHENTICATED") != null &&
                (boolean) session.getAttribute("TOTP_AUTHENTICATED")) {
            return "home";           // якщо так — показ головної сторінки
        } else {
            return "redirect:/totp"; // якщо ні — перенаправлення користувача назад
        }
    }
}
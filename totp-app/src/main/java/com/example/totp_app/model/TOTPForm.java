package com.example.totp_app.model;

// Клас для зберігання введеного користувачем TOTP-коду
public class TOTPForm {
    // Поле для зберігання введеного коду
    private String code;
    // Геттер для отримання значення коду
    public String getCode() {
        return code;
    }
    // Сеттер для встановлення значення коду
    public void setCode(String code) {
        this.code = code;
    }
}
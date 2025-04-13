package com.example.totp_app;

import com.example.totp_app.service.TOTPService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication // основна анотація, що вмикає автоматичну конфігурацію Spring Boot
public class TotpAppApplication {
	public static void main(String[] args) {
		// Запуск додатку Spring Boot
		// (фреймворк, що спрощує створення веб-додатків та мікросервісів на Java)
		SpringApplication.run(TotpAppApplication.class, args);
	}

	@Bean
	public CommandLineRunner startTOTPGeneration(TOTPService totpService) {
		return args -> {
			// Запускається потік, що буде постійно генерувати та виводити TOTP-коди
			totpService.startGeneratingTotp();
		};
	}
}
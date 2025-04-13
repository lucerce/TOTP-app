package com.example.totp_app.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@EnableWebSecurity
// Клас, що містить налаштування безпеки для всієї програми
public class SecurityConfig {

    @Bean
    // Налаштування правил доступу та входу в систему
    public SecurityFilterChain securityFilter(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable()) // відключення захисту від CSRF-атак
                // Налаштування доступу до сторінок
                .authorizeHttpRequests(auth -> auth
                        // Дозвіл на доступ до сторінок входу та введення TOTP без авторизації
                        .requestMatchers("/login", "/totp", "/totp-verify").permitAll()
                        // Усі інші сторінки вимагають авторизації
                        .anyRequest().authenticated()
                )
                // Налаштування сторінки входу в систему
                .formLogin(form -> form
                        // Встановлення сторінки входу
                        .loginPage("/login")
                        // Після успішного входу, відбувається перенаправлення на сторінку введення TOTP-коду
                        .defaultSuccessUrl("/totp", true)
                        .permitAll()
                )
                .logout(logout -> logout.permitAll()); // дозвіл для всіх користувачам на вихід із системи
        return http.build();
    }

    @Bean
    // Налаштування шифрування паролів
    public PasswordEncoder passwordEncoder() {
        // Створення об'єкта для шифрування паролів, який підтримує різні алгоритми
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    // Створення користувача у пам'яті
    public UserDetailsService userDetailsService(PasswordEncoder passwordEncoder) {
        // Користувач з іменем "lucerce" і паролем "22365"
        UserDetails user = User.builder()
                .username("lucerce")
                .password(passwordEncoder.encode("22365")) // пароль шифрується перед збереженням
                .roles("USER")
                .build();
        return new InMemoryUserDetailsManager(user);
    }
}
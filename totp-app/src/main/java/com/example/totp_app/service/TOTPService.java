package com.example.totp_app.service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
// Клас-сервіс, який займається створенням одноразових кодів (TOTP)
public class TOTPService {

    private final byte[] secretKey;    // секретний ключ для генерації TOTP
    private final int digits = 6;      // кількість цифр у TOTP-коді
    private final int timeStep = 30;   // час, протягом якого код дійсний (сек)
    private final String algorithm;    // алгоритм хешування
    private String currentTotp = null; // поточний згенерований TOTP-код

    // Конструктор, де алгоритм можна задати через application.properties
    // (за замовчуванням стоїть алгоритм хешування HmacSHA1)
    public TOTPService(@Value("${totp.algorithm:HmacSHA1}") String algorithm) {
        this.algorithm = algorithm;
        this.secretKey = generateSecretKey(algorithm);
    }

    // Генерація випадкового секретного ключа відповідного розміру залежно від алгоритму
    private byte[] generateSecretKey(String algorithm) {
        int keySize;
        switch (algorithm) {
            case "HmacSHA256": keySize = 32; break;
            case "HmacSHA512": keySize = 64; break;
            default: keySize = 20; // HmacSHA1
        }
        byte[] key = new byte[keySize];
        new SecureRandom().nextBytes(key); // заповнення випадковими значеннями
        return key;
    }

    // Основний метод генерації TOTP-коду
    public String generateTOTP(byte[] secretKey, long time, int digits, int timeStep, String algorithm) {
        long T = (time / 1000) / timeStep; // обчислення поточного часового кроку
        byte[] data = ByteBuffer.allocate(8).putLong(T).array(); // цей часовий крок перетворюється у набір байтів

        try {
            // Ініціалізація HMAC з вказаним алгоритмом і секретним ключем
            Mac mac = Mac.getInstance(algorithm);
            SecretKeySpec signKey = new SecretKeySpec(secretKey, algorithm);
            mac.init(signKey);
            byte[] hash = mac.doFinal(data);           // обчислення хешу (залежить від часу і секретного ключа)
            int offset = hash[hash.length - 1] & 0x0F; // обирається певна частина хешу
            // Формування 4-байтного числа з цього хешу
            int binary = ((hash[offset] & 0x7F) << 24) |
                     ((hash[offset + 1] & 0xFF) << 16) |
                     ((hash[offset + 2] & 0xFF) << 8)  |
                      (hash[offset + 3] & 0xFF);
            // Лишаються лише останні 6 цифр
            int otp = binary % (int) Math.pow(10, digits);
            // Цей пароль зберігається і додає ведучі нулі, якщо треба
            currentTotp = String.format("%0" + digits + "d", otp);
        } catch (GeneralSecurityException e) {
            throw new RuntimeException("Error during TOTP generation", e);
        }
        return currentTotp;
    }

    // Спрощена версія методу generateTOTP
    // (він не вимагає кожного разу передавати секретний ключ і алгоритм)
    // (дані зберігаються один раз у полях об'єкта, а цей метод просто підставляє їх в основний метод)
    public String generateTOTP(long time, int digits, int timeStep) {
        return generateTOTP(secretKey, time, digits, timeStep, algorithm);
    }

    // Метод, який повертає останній створений код
    // (завдяки йому можна бачити поточний TOTP-код у консолі)
    public String getCurrentTotp() {
        return currentTotp;
    }

    // Запуск потоку, який кожну секунду показує в консолі поточний пароль
    public void startGeneratingTotp() {
        new Thread(() -> {
            long lastPrintedStep = -1; // збереження останнього кроку
            while (true) {
                try {
                    long currentTime = System.currentTimeMillis(); // поточний час
                    long currentStep = (currentTime / 1000) / timeStep; // підрахунок поточного часового кроку
                    int secondsRemaining = timeStep - (int)((currentTime / 1000) % timeStep); // підрахунок часу до нового паролю
                    // Якщо крок змінився - генерується новий код
                    if (currentStep != lastPrintedStep) {
                        generateTOTP(secretKey, currentTime, digits, timeStep, algorithm);
                        lastPrintedStep = currentStep;
                    }
                    // У консоль виводиться поточний пароль і скільки він ще буде дійсний
                    System.out.print("\rGenerated TOTP (valid for " + secondsRemaining + " seconds): " + getCurrentTotp());
                    System.out.flush();
                    // Зупинка на 1 секунду, щоб не перевантажувати програму
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt(); // якщо потік хтось зупинив - вихід з циклу
                    break;
                }
            }
        }).start();
    }
}
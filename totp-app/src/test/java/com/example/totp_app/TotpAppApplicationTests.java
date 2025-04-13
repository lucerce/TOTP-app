package com.example.totp_app;

import com.example.totp_app.service.TOTPService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Import(tests.TestConfig.class)
class tests {

	@Autowired
	private MockMvc mockMvc; // емуляція HTTP-запитів

	@Autowired
	private TOTPService totpService; // генерація TOTP

	// Тестова конфігурація, яка замінює реальний TOTPService на мок-об'єкт
	// (мок - це фейковий об’єкт, який імітує поведінку реального сервісу)
	@TestConfiguration
	static class TestConfig {
		@Bean
		public TOTPService totpService() {
			return Mockito.mock(TOTPService.class); // створення мок-сервісу
		}
	}

	// --- 1. Тест завантаження контексту програми ---
	@Test
	void contextLoads() {
	// Якщо контекст програми завантажується без помилок, то тест пройдено успішно
	}

	// --- 2. Тести для TOTPService ---
	@Test
	void testTOTPLength() {
		// Створення локального об'єкта TOTPService з алгоритмом HmacSHA1
		TOTPService localTotpService = new TOTPService("HmacSHA1");
		long currentTime = System.currentTimeMillis();
		// Генерація TOTP-код
		String totp = localTotpService.generateTOTP(currentTime, 6, 30);

		assertNotNull(totp, "TOTP не повинен бути null");
		assertEquals(6, totp.length(), "TOTP повинен містити 6 цифр");
		assertTrue(totp.matches("\\d{6}"), "TOTP повинен складатися тільки з цифр");
	}

	@Test
	// Тест, де при однаковому часі TOTP-коди збігаються
	void testSameTime() {
		TOTPService localTotpService = new TOTPService("HmacSHA1");
		long currentTime = System.currentTimeMillis();

		String totp1 = localTotpService.generateTOTP(currentTime, 6, 30);
		String totp2 = localTotpService.generateTOTP(currentTime, 6, 30);

		assertEquals(totp1, totp2, "TOTP, згенеровані в один і той же час, повинні збігатися");
	}

	@Test
	// Тест, де при різних часах TOTP-коди різні
	void testDifferentTime() {
		TOTPService localTotpService = new TOTPService("HmacSHA1");
		long currentTime = System.currentTimeMillis();

		String totp1 = localTotpService.generateTOTP(currentTime, 6, 30);
		String totp2 = localTotpService.generateTOTP(currentTime + 30000, 6, 30); // + 30000 мілісекунд (30 секунд)

		assertNotEquals(totp1, totp2, "TOTP, згенеровані в різний час, не повинні збігатися");
	}

	// --- 3. Тести для TOTPController ---
	@Test
	// Налаштовування поведінки мок-об'єкта totpService:
	// при виклику методу generateTOTP повертається рядок "123456"
	void testPage() throws Exception {
		Mockito.when(totpService.generateTOTP(anyLong(), eq(6), eq(30)))
				.thenReturn("123456");

		mockMvc.perform(get("/totp"))
				.andExpect(status().isOk())                              	// очікується статус 200 (ОК)
				.andExpect(view().name("totp"))         	//  очікується, що буде повернута сторінка "totp"
				.andExpect(model().attributeExists("totpForm"));	//  перевірка, чи є в моделі атрибут totpForm
	}

	@Test
	void testVerifySuccess() throws Exception {
		// Налаштовання мок-об'єкта totpService так, що поточний код буде "123456"
		Mockito.when(totpService.getCurrentTotp()).thenReturn("123456");
		// Створення сесії для тестування
		MockHttpSession session = new MockHttpSession();
		// Спочатку користувач не автентифікований
		session.setAttribute("TOTP_AUTHENTICATED", false);
		// Відправка POST-запит на /totp-verify з правильним кодом "123456"
		mockMvc.perform(post("/totp-verify")
						.session(session)
						.param("code", "123456"))
				.andExpect(status().is3xxRedirection())         // очікування редиректу
				.andExpect(redirectedUrl("/home"));  // редирект має вести на сторінку /home
	}

	@Test
	void testVerifyFail() throws Exception {
		// Налаштовання мок-об'єкта totpService так, що поточний код буде "123456"
		Mockito.when(totpService.getCurrentTotp()).thenReturn("123456");
		// Створення сесії для тестування
		MockHttpSession session = new MockHttpSession();
		// Відправка POST-запит на /totp-verify з неправильним кодом "000000"
		mockMvc.perform(post("/totp-verify")
						.session(session)
						.param("code", "000000"))
				.andExpect(status().is3xxRedirection())					  // очікування редиректу
				.andExpect(redirectedUrl("/totp?error=true")); // редирект має вести назад на сторінку /totp з параметром помилки
	}

	// --- 4. Тести для SecurityConfig ---
	@Test
	// Перевірка, чи доступна сторінка логіну (/login)
	void testLogin() throws Exception {
		mockMvc.perform(get("/login"))
				.andExpect(status().isOk());
	}

	@Test
	// Перевірка, чи доступна сторінка для TOTP (/totp) без авторизації
	void testAccessible() throws Exception {
		mockMvc.perform(get("/totp"))
				.andExpect(status().isOk());
	}

	@Test
	// Перевірка, чи недоступна сторінка /home без авторизації
	void testAuthentication() throws Exception {
		mockMvc.perform(get("/home"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrlPattern("**/login"));
	}

	// --- 5. Тест для LoginController ---
	@Test
	// Перевірка, чи повертається сторінка "login" при зверненні до /login
	void testLoginPage() throws Exception {
		mockMvc.perform(get("/login"))
				.andExpect(status().isOk())
				.andExpect(view().name("login"));
	}
}
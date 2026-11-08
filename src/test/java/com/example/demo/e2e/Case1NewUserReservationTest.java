package com.example.demo.e2e;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class Case1NewUserReservationTest {

    private WebDriver driver;
    private WebDriverWait wait;
    private final String BASE_URL = "http://localhost:4200";

    @BeforeEach
    public void init() {
        WebDriverManager.chromedriver().setup();

        ChromeOptions chromeOptions = new ChromeOptions();
        Map<String, Object> prefs = new HashMap<>();
        prefs.put("credentials_enable_service", false);
        prefs.put("profile.password_manager_enabled", false);
        prefs.put("profile.password_manager_leak_detection", false);
        chromeOptions.setExperimentalOption("prefs", prefs);
        chromeOptions.addArguments("--incognito");
        chromeOptions.addArguments("--disable-notifications");
        chromeOptions.addArguments("--disable-extensions");
        chromeOptions.addArguments("--disable-save-password-bubble");
        chromeOptions.addArguments("--disable-features=PasswordLeakDetection");
        // chromeOptions.addArguments("--headless");

        driver = new ChromeDriver(chromeOptions);
        wait = new WebDriverWait(driver, Duration.ofSeconds(10));
    }

    @AfterEach
    void tearDown() {
        if (driver != null) {
            driver.quit();
        }
    }

    @Test
    public void Case1_nuevoUsuario_registroError_dosReservasSolapadas() {
        // 1. Llegar al Landing Page
        driver.get(BASE_URL + "/");

        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("nav-login-link")));
        WebElement navLogin = driver.findElement(By.id("nav-login-link"));
        navLogin.click();

        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("create-account-btn")));
        WebElement btnCrearCuenta = driver.findElement(By.id("create-account-btn"));
        btnCrearCuenta.click();

        // 2. Registro con correo invalido
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("client-firstName")));

        WebElement inputNombre = driver.findElement(By.id("client-firstName"));
        WebElement inputApellido = driver.findElement(By.id("client-lastName"));
        WebElement inputUsername = driver.findElement(By.id("client-username"));
        WebElement inputEmail = driver.findElement(By.id("client-email"));
        WebElement inputPassword = driver.findElement(By.id("client-password"));
        WebElement inputPhone = driver.findElement(By.id("client-phone"));

        inputNombre.sendKeys("Carlos");
        inputApellido.sendKeys("Mendoza");

        String usuarioUnico = "carlos" + System.currentTimeMillis();
        inputUsername.sendKeys(usuarioUnico);
        inputEmail.sendKeys("correo-invalido-sin-arroba");
        inputPassword.sendKeys("12345");
        inputPhone.sendKeys("3001234567");

        WebElement btnSubmit = driver.findElement(By.id("client-submit-btn"));
        btnSubmit.click();

        // Verificar el mensaje de error del correo
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("client-email-error")));
        WebElement errorEmail = driver.findElement(By.id("client-email-error"));
        Assertions.assertThat(errorEmail.getText()).containsIgnoringCase("Ingresa un correo electrónico válido");

        // 3. Corregir correo y registrarse correctamente
        inputEmail.clear();
        inputEmail.sendKeys(usuarioUnico + "@javeriana.edu.co");
        btnSubmit.click();

        // Redirige al perfil del usuario
        wait.until(ExpectedConditions.urlContains("/profile/"));

        // 4. Generar primera reserva para la siguiente semana
        LocalDate fechaHoy = LocalDate.now();
        LocalDate checkIn1 = fechaHoy.plusDays(7);
        LocalDate checkOut1 = fechaHoy.plusDays(10);

        driver.get(BASE_URL + "/reservations/book?checkIn=" + checkIn1 + "&checkOut=" + checkOut1);

        wait.until(ExpectedConditions.presenceOfElementLocated(By.className("room-type-option")));
        Select selectTipo1 = new Select(driver.findElement(By.id("booking-roomTypeId")));
        selectTipo1.selectByIndex(0);

        WebElement btnReservar1 = driver.findElement(By.id("btn-confirmar-reserva"));
        btnReservar1.click();

        // Esperar a que la reserva aparezca en el perfil
        wait.until(ExpectedConditions.urlContains("/profile/"));
        wait.until(ExpectedConditions.presenceOfElementLocated(By.className("reservation-card-item")));

        List<WebElement> listaInicial = driver.findElements(By.className("reservation-card-item"));
        Assertions.assertThat(listaInicial.size()).isEqualTo(1);

        WebElement tarjeta1 = listaInicial.get(0);
        String habitacion1 = tarjeta1.findElement(By.className("res-card-room-number"))
                .getText().replace("(Hab. ", "").replace(")", "").trim();

        Assertions.assertThat(habitacion1).isNotEmpty();

        // 5. Segunda reserva que se intercepta con las fechas de la primera (solapamiento)
        LocalDate checkIn2 = fechaHoy.plusDays(8);
        LocalDate checkOut2 = fechaHoy.plusDays(11);

        driver.get(BASE_URL + "/reservations/book?checkIn=" + checkIn2 + "&checkOut=" + checkOut2);

        wait.until(ExpectedConditions.presenceOfElementLocated(By.className("room-type-option")));
        Select selectTipo2 = new Select(driver.findElement(By.id("booking-roomTypeId")));
        selectTipo2.selectByIndex(0);

        WebElement btnReservar2 = driver.findElement(By.id("btn-confirmar-reserva"));
        btnReservar2.click();

        // Esperar que la lista de reservas aumente en 1 con el lambda
        wait.until(driver -> driver.findElements(By.className("reservation-card-item")).size() == listaInicial.size() + 1);

        List<WebElement> listaFinal = driver.findElements(By.className("reservation-card-item"));
        Assertions.assertThat(listaFinal.size()).isEqualTo(listaInicial.size() + 1);

        // Obtener la habitacion de la segunda reserva (la ultima de la lista)
        WebElement tarjeta2 = listaFinal.get(listaFinal.size() - 1);
        String habitacion2 = tarjeta2.findElement(By.className("res-card-room-number"))
                .getText().replace("(Hab. ", "").replace(")", "").trim();

        // Asegurar que el numero de habitacion asignado sea diferente al primero
        Assertions.assertThat(habitacion2).isNotEqualTo(habitacion1);
        Assertions.assertThat(habitacion2).isNotEmpty();
    }
}

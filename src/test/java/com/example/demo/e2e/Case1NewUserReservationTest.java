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
import java.util.Set;
import java.util.stream.Collectors;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class Case1NewUserReservationTest {

    private WebDriver driver;
    private WebDriverWait wait;
    private final String BASE_URL = "http://localhost:4200";
    private final By RESERVAS = By.className("reservation-card-item");

    @BeforeEach
    public void init() {
        WebDriverManager.chromedriver().setup();
        ChromeOptions options = new ChromeOptions();
        Map<String, Object> prefs = new HashMap<>();
        prefs.put("credentials_enable_service", false);
        prefs.put("profile.password_manager_enabled", false);
        options.setExperimentalOption("prefs", prefs);
        options.addArguments("--disable-notifications", "--disable-extensions");
        // options.addArguments("--headless=new");
        driver = new ChromeDriver(options);
        wait = new WebDriverWait(driver, Duration.ofSeconds(15));
    }

    @AfterEach
    public void tearDown() {
        if (driver != null) driver.quit();
    }

    private WebElement visible(By selector) {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(selector));
    }

    private void click(By selector) {
        wait.until(ExpectedConditions.elementToBeClickable(selector)).click();
    }

    private String idReserva(WebElement tarjeta) {
        return tarjeta.findElement(By.className("res-card-id"))
                .getText().replace("#", "").trim();
    }

    private String numeroHabitacion(WebElement tarjeta) {
        return tarjeta.findElement(By.className("res-card-room-number"))
                .getText().replaceAll("[^0-9]", "");
    }

    private Map<String, String> reservasPorId() {
        return driver.findElements(RESERVAS).stream().collect(Collectors.toMap(
                this::idReserva, this::numeroHabitacion));
    }

    private String buscarNuevaReserva(Set<String> idsIniciales) {
        return wait.until(d -> {
            Set<String> actuales = reservasPorId().keySet();
            Set<String> nuevos = actuales.stream().filter(id -> !idsIniciales.contains(id))
                    .collect(Collectors.toSet());
            return nuevos.size() == 1 ? nuevos.iterator().next() : null;
        });
    }

    @Test
    public void Case1_nuevoUsuario_registroError_dosReservasSolapadas() {
        // 1. Landing page y formulario de registro.
        driver.get(BASE_URL + "/");
        click(By.id("nav-login-link"));
        click(By.id("create-account-btn"));

        visible(By.id("client-firstName")).sendKeys("Carlos");
        driver.findElement(By.id("client-lastName")).sendKeys("Mendoza");
        String usuario = "carlos" + System.currentTimeMillis();
        driver.findElement(By.id("client-username")).sendKeys(usuario);
        WebElement correo = driver.findElement(By.id("client-email"));
        correo.sendKeys("correo-invalido-sin-arroba");
        driver.findElement(By.id("client-password")).sendKeys("12345");
        driver.findElement(By.id("client-phone")).sendKeys("3001234567");
        click(By.id("client-submit-btn"));

        // 2. Debe aparecer el error del correo inválido.
        wait.until(d -> visible(By.id("client-email-error")).getText().toLowerCase()
                .contains("correo electrónico válido"));
        Assertions.assertThat(driver.findElement(By.id("client-email-error")).getText())
                .containsIgnoringCase("correo electrónico válido");

        // 3. Corregir el correo y registrarse.
        correo.clear();
        correo.sendKeys(usuario + "@javeriana.edu.co");
        click(By.id("client-submit-btn"));
        wait.until(ExpectedConditions.urlContains("/profile/"));

        // 4. Primera reserva para la próxima semana.
        LocalDate hoy = LocalDate.now();
        LocalDate entrada1 = hoy.plusDays(7);
        LocalDate salida1 = hoy.plusDays(10);
        driver.get(BASE_URL + "/reservations/book?checkIn=" + entrada1 + "&checkOut=" + salida1);
        wait.until(ExpectedConditions.numberOfElementsToBeMoreThan(By.className("room-type-option"), 0));
        new Select(driver.findElement(By.id("booking-roomTypeId"))).selectByIndex(0);
        click(By.id("btn-confirmar-reserva"));
        wait.until(ExpectedConditions.urlContains("/profile/"));
        wait.until(d -> !d.findElements(RESERVAS).isEmpty());

        Map<String, String> reservasPrimera = reservasPorId();
        Assertions.assertThat(reservasPrimera).hasSize(1);
        String idPrimera = reservasPrimera.keySet().iterator().next();
        String habitacion1 = reservasPrimera.get(idPrimera);
        Assertions.assertThat(habitacion1).isNotBlank();
        int cantidadInicial = reservasPrimera.size();

        // 5. Buscar fechas que se solapan con la primera reserva.
        LocalDate entrada2 = hoy.plusDays(8);
        LocalDate salida2 = hoy.plusDays(11);
        driver.get(BASE_URL + "/reservations/book?checkIn=" + entrada2 + "&checkOut=" + salida2);
        wait.until(ExpectedConditions.numberOfElementsToBeMoreThan(By.className("room-type-option"), 0));
        new Select(driver.findElement(By.id("booking-roomTypeId"))).selectByIndex(0);

        // La primera habitación debe figurar como no disponible.
        wait.until(d -> d.findElements(By.className("room-unavailable-badge")).stream()
                .anyMatch(b -> b.getText().matches("(?is).*Hab\\.\\s*" + habitacion1 + "\\s*:\\s*No disponible.*")));
        List<WebElement> noDisponibles = driver.findElements(By.className("room-unavailable-badge"));
        Assertions.assertThat(noDisponibles.stream().map(WebElement::getText).toList())
                .anyMatch(t -> t.matches("(?is).*Hab\\.\\s*" + habitacion1 + "\\s*:\\s*No disponible.*"));

        // 6. Crear segunda reserva y localizarla por ID, no por posición.
        click(By.id("btn-confirmar-reserva"));
        wait.until(ExpectedConditions.urlContains("/profile/"));
        wait.until(d -> d.findElements(RESERVAS).size() == cantidadInicial + 1);
        String idSegunda = buscarNuevaReserva(reservasPrimera.keySet());
        Map<String, String> reservasFinales = reservasPorId();
        Assertions.assertThat(reservasFinales).hasSize(cantidadInicial + 1);
        Assertions.assertThat(idSegunda).isNotEqualTo(idPrimera);
        String habitacion2 = reservasFinales.get(idSegunda);
        Assertions.assertThat(habitacion2).isNotBlank().isNotEqualTo(habitacion1);
        Assertions.assertThat(reservasFinales.get(idPrimera)).isEqualTo(habitacion1);
    }
}

package com.example.demo.e2e;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.WindowType;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class Case2UserOperatorStayCycleTest {

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
    public void Case2_usuarioYOperador_cicloEstadiaCompleto() {
        // 1. Usuario ya registrado realiza login con su perfil
        driver.get(BASE_URL + "/login");

        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("login-identifier")));
        WebElement inputUser = driver.findElement(By.id("login-identifier"));
        WebElement inputPass = driver.findElement(By.id("login-password"));

        inputUser.sendKeys("demo");
        inputPass.sendKeys("demo");

        WebElement btnLogin = driver.findElement(By.id("login-submit-btn"));
        btnLogin.click();

        // 2. Revisa sus proximas reservas y aparece una que realizo tiempo atras que esta sin iniciar
        wait.until(ExpectedConditions.urlContains("/profile/"));
        wait.until(ExpectedConditions.presenceOfElementLocated(By.className("reservation-card-item")));

        List<WebElement> reservasUsuario = driver.findElements(By.className("reservation-card-item"));
        Assertions.assertThat(reservasUsuario.size()).isGreaterThanOrEqualTo(1);

        WebElement primeraReserva = reservasUsuario.get(0);
        WebElement badgeEstado = primeraReserva.findElement(By.className("res-card-status"));
        Assertions.assertThat(badgeEstado.getText()).containsIgnoringCase("Confirmada");

        // Guardar la referencia a la pestana del usuario
        String pestanaUsuario = driver.getWindowHandle();

        // 3. En otra pestana ingresa un operador con su usuario y contrasena
        driver.switchTo().newWindow(WindowType.TAB);

        driver.get(BASE_URL + "/login");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("login-identifier")));

        driver.findElement(By.id("login-identifier")).sendKeys("operario");
        driver.findElement(By.id("login-password")).sendKeys("operario");
        driver.findElement(By.id("login-submit-btn")).click();

        // 4. Va al perfil de reservas y activa (checkin) la reserva del usuario
        wait.until(ExpectedConditions.urlContains("/operator/reservations"));
        wait.until(ExpectedConditions.presenceOfElementLocated(By.className("btn-activar-estadia")));

        WebElement btnActivar = driver.findElement(By.className("btn-activar-estadia"));
        btnActivar.click();

        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("modal-confirm-btn")));
        WebElement btnConfirmarModal = driver.findElement(By.id("modal-confirm-btn"));
        btnConfirmarModal.click();

        // Esperar a que la reserva cambie a estado Activa
        wait.until(ExpectedConditions.presenceOfElementLocated(By.className("status-activa")));

        // 5. Agrega 2 servicios a esta reserva
        WebElement btnContratar = driver.findElement(By.className("btn-contratar-servicio"));
        btnContratar.click();

        wait.until(ExpectedConditions.presenceOfElementLocated(By.className("service-option")));
        List<WebElement> serviciosIniciales = driver.findElements(By.className("service-account-item"));
        String montoInicial = driver.findElement(By.id("modal-total-account-price")).getText();

        Select selectServicio = new Select(driver.findElement(By.id("select-service-modal")));
        WebElement btnAgregarServicio = driver.findElement(By.id("btn-confirm-add-service"));

        // Servicio 1
        selectServicio.selectByIndex(0);
        btnAgregarServicio.click();

        wait.until(driver -> driver.findElements(By.className("service-account-item")).size() == serviciosIniciales.size() + 1);

        // Servicio 2
        selectServicio.selectByIndex(1);
        driver.findElement(By.id("btn-confirm-add-service")).click();

        wait.until(driver -> driver.findElements(By.className("service-account-item")).size() == serviciosIniciales.size() + 2);

        List<WebElement> serviciosFinales = driver.findElements(By.className("service-account-item"));
        Assertions.assertThat(serviciosFinales.size()).isEqualTo(serviciosIniciales.size() + 2);

        String montoTotalConServicios = driver.findElement(By.id("modal-total-account-price")).getText();
        Assertions.assertThat(montoTotalConServicios).isNotEqualTo(montoInicial);

        WebElement btnCerrarModalServicios = driver.findElement(By.id("btn-close-service-modal"));
        btnCerrarModalServicios.click();

        // 6. El usuario acaba su estadia y decide pagar todos los servicios pendientes
        wait.until(ExpectedConditions.presenceOfElementLocated(By.className("btn-pagar-cuenta")));
        WebElement btnPagar = driver.findElement(By.className("btn-pagar-cuenta"));
        btnPagar.click();

        // Verificar que el monto a pagar en el modal sea el apropiado (incluyendo los 2 servicios agregados)
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("modal-description")));
        WebElement descripcionModal = driver.findElement(By.id("modal-description"));
        Assertions.assertThat(descripcionModal.getText()).contains(montoTotalConServicios);

        // Confirmar el pago
        driver.findElement(By.id("modal-confirm-btn")).click();

        // Esperar a que aparezca la etiqueta de cuenta pagada
        wait.until(ExpectedConditions.presenceOfElementLocated(By.className("cuenta-pagada-badge")));

        // 7. La reserva se da por finalizada (checkout)
        wait.until(ExpectedConditions.presenceOfElementLocated(By.className("btn-finalizar-estadia")));
        WebElement btnFinalizar = driver.findElement(By.className("btn-finalizar-estadia"));
        btnFinalizar.click();

        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("modal-confirm-btn")));
        driver.findElement(By.id("modal-confirm-btn")).click();

        // Verificar que el estado cambie a Inactiva
        wait.until(ExpectedConditions.presenceOfElementLocated(By.className("status-inactiva")));

        WebElement estadoFinalTabla = driver.findElement(By.className("status-inactiva"));
        Assertions.assertThat(estadoFinalTabla.getText()).containsIgnoringCase("Inactiva");

        // 8. Verificar que en la pestana del usuario se vea finalizada y pagada
        driver.switchTo().window(pestanaUsuario);
        driver.navigate().refresh();

        wait.until(ExpectedConditions.presenceOfElementLocated(By.className("reservation-card-item")));
        WebElement tarjetaPerfil = driver.findElements(By.className("reservation-card-item")).get(0);

        Assertions.assertThat(tarjetaPerfil.findElement(By.className("res-card-status")).getText())
                .containsIgnoringCase("Finalizada");
        Assertions.assertThat(tarjetaPerfil.findElement(By.className("res-card-paid-badge")).getText())
                .containsIgnoringCase("Pagada");
    }
}

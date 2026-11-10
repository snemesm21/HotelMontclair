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

import java.math.BigDecimal;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class Case2UserOperatorStayCycleTest {

    private WebDriver driverUsuario;
    private WebDriver driverOperador;
    private WebDriverWait waitUsuario;
    private WebDriverWait waitOperador;
    private final String BASE_URL = "http://localhost:4200";

    @BeforeEach
    public void init() {
        WebDriverManager.chromedriver().setup();
        driverUsuario = crearChrome();
        driverOperador = crearChrome();
        waitUsuario = new WebDriverWait(driverUsuario, Duration.ofSeconds(15));
        waitOperador = new WebDriverWait(driverOperador, Duration.ofSeconds(15));
    }

    private WebDriver crearChrome() {
        ChromeOptions options = new ChromeOptions();
        Map<String, Object> prefs = new HashMap<>();
        prefs.put("credentials_enable_service", false);
        prefs.put("profile.password_manager_enabled", false);
        prefs.put("profile.password_manager_leak_detection", false);
        options.setExperimentalOption("prefs", prefs);
        options.addArguments("--disable-notifications", "--disable-extensions",
                "--disable-features=PasswordLeakDetection");
        // options.addArguments("--headless=new");
        return new ChromeDriver(options);
    }

    @AfterEach
    public void tearDown() {
        if (driverUsuario != null) driverUsuario.quit();
        if (driverOperador != null) driverOperador.quit();
    }

    private void login(WebDriver driver, WebDriverWait wait, String usuario, String clave) {
        driver.get(BASE_URL + "/login");
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("login-identifier"))).sendKeys(usuario);
        driver.findElement(By.id("login-password")).sendKeys(clave);
        wait.until(ExpectedConditions.elementToBeClickable(By.id("login-submit-btn"))).click();
    }

    private WebElement fila(String id) {
        return driverOperador.findElement(By.cssSelector("tr.reservation-table-row[data-id='" + id + "']"));
    }

    private By selectorFila(String id) {
        return By.cssSelector("tr.reservation-table-row[data-id='" + id + "']");
    }

    private void clickFila(String id, String claseBoton) {
        By boton = By.cssSelector("tr.reservation-table-row[data-id='" + id + "'] ." + claseBoton);
        waitOperador.until(ExpectedConditions.elementToBeClickable(boton)).click();
    }

    private void confirmarModal() {
        waitOperador.until(ExpectedConditions.elementToBeClickable(By.id("modal-confirm-btn"))).click();
    }

    private BigDecimal dinero(String texto) {
        // Formato que usa Angular: 1.234,50 €
        String valor = texto.replaceAll("[^0-9,.-]", "")
                .replace(".", "").replace(',', '.');
        return new BigDecimal(valor);
    }

    private BigDecimal precioOpcion(String texto) {
        // Las opciones tienen formato: Nombre — 25,00 €
        String[] partes = texto.split("—");
        Assertions.assertThat(partes).hasSizeGreaterThan(1);
        return dinero(partes[partes.length - 1]);
    }

    private WebElement tarjetaUsuario(String id) {
        return driverUsuario.findElements(By.className("reservation-card-item")).stream()
                .filter(t -> t.findElement(By.className("res-card-id"))
                        .getText().replace("#", "").trim().equals(id))
                .findFirst().orElseThrow(() -> new AssertionError("No aparece la reserva #" + id));
    }

    @Test
    public void Case2_usuarioYOperador_cicloEstadiaCompleto() {
        // 1. El cliente inicia sesión y consulta sus reservas.
        login(driverUsuario, waitUsuario, "demo", "demo");
        waitUsuario.until(ExpectedConditions.urlContains("/profile/"));
        // Esperar específicamente la reserva confirmada, no simplemente cualquier tarjeta.
        By selectorConfirmada = By.cssSelector(".reservation-card-item .res-card-status");
        try {
            waitUsuario.until(d -> d.findElements(selectorConfirmada).stream()
                    .anyMatch(estado -> estado.getText().trim().equalsIgnoreCase("Confirmada")));
        } catch (org.openqa.selenium.TimeoutException ex) {
            List<WebElement> encontradas = driverUsuario.findElements(By.className("reservation-card-item"));
            StringBuilder detalle = new StringBuilder();
            for (WebElement tarjeta : encontradas) {
                detalle.append(" [")
                        .append(tarjeta.findElement(By.className("res-card-id")).getText())
                        .append(" / ")
                        .append(tarjeta.findElement(By.className("res-card-status")).getText())
                        .append("]");
            }
            throw new AssertionError("No apareció una reserva confirmada para demo. "
                    + "URL: " + driverUsuario.getCurrentUrl()
                    + "; tarjetas encontradas: " + encontradas.size()
                    + "; estados:" + detalle, ex);
        }
        List<WebElement> reservas = driverUsuario.findElements(By.className("reservation-card-item"));
        WebElement confirmada = reservas.stream()
                .filter(t -> t.findElement(By.className("res-card-status"))
                        .getText().trim().equalsIgnoreCase("Confirmada"))
                .findFirst().orElseThrow(() -> new AssertionError("No hay reserva CONFIRMED visible"));
        String idReserva = confirmada.findElement(By.className("res-card-id"))
                .getText().replace("#", "").trim();
        Assertions.assertThat(idReserva).isNotBlank();

        // 2. Operador inicia sesión en un Chrome independiente.
        login(driverOperador, waitOperador, "operario", "operario");
        waitOperador.until(ExpectedConditions.urlContains("/operator/reservations"));
        waitOperador.until(ExpectedConditions.presenceOfElementLocated(selectorFila(idReserva)));
        Assertions.assertThat(fila(idReserva).findElement(By.className("res-row-status"))
                .getText()).containsIgnoringCase("Confirmada");

        // 3. Check-in de la reserva correcta.
        clickFila(idReserva, "btn-activar-estadia");
        confirmarModal();
        // El operador actualiza su tabla automáticamente al confirmar el check-in.
        waitOperador.until(d -> {
            List<WebElement> estados = d.findElements(By.cssSelector(
                    "tr.reservation-table-row[data-id='" + idReserva + "'] .res-row-status"));
            return !estados.isEmpty() && estados.get(0).getText().trim().equalsIgnoreCase("Activa");
        });
        Assertions.assertThat(fila(idReserva).findElement(By.className("res-row-status"))
                .getText()).containsIgnoringCase("Activa");

        // La página del cliente no se actualiza automáticamente: recargar SU navegador.
        driverUsuario.navigate().refresh();
        waitUsuario.until(d -> d.findElements(By.className("reservation-card-item")).stream()
                .anyMatch(t -> {
                    List<WebElement> ids = t.findElements(By.className("res-card-id"));
                    List<WebElement> estados = t.findElements(By.className("res-card-status"));
                    return !ids.isEmpty() && !estados.isEmpty()
                            && ids.get(0).getText().replace("#", "").trim().equals(idReserva)
                            && estados.get(0).getText().trim().equalsIgnoreCase("En Estadía (Activa)");
                }));
        Assertions.assertThat(tarjetaUsuario(idReserva).findElement(By.className("res-card-status"))
                .getText()).containsIgnoringCase("Activa");

        // 4. Agregar exactamente dos servicios y comprobar su importe.
        clickFila(idReserva, "btn-contratar-servicio");
        waitOperador.until(ExpectedConditions.visibilityOfElementLocated(By.id("select-service-modal")));
        int cantidadInicial = driverOperador.findElements(By.className("service-account-item")).size();
        BigDecimal montoInicial = dinero(driverOperador.findElement(By.id("modal-total-account-price")).getText());
        Select select = new Select(driverOperador.findElement(By.id("select-service-modal")));
        Assertions.assertThat(select.getOptions().size()).isGreaterThanOrEqualTo(2);

        BigDecimal precio1 = precioOpcion(select.getOptions().get(0).getText());
        BigDecimal precio2 = precioOpcion(select.getOptions().get(1).getText());
        WebElement cantidad = driverOperador.findElement(By.id("input-service-quantity"));
        cantidad.clear();
        cantidad.sendKeys("1");

        select.selectByIndex(0);
        waitOperador.until(ExpectedConditions.elementToBeClickable(By.id("btn-confirm-add-service"))).click();
        waitOperador.until(d -> d.findElements(By.className("service-account-item")).size() == cantidadInicial + 1);

        // El modal se actualiza con la respuesta del servidor.
        select = new Select(driverOperador.findElement(By.id("select-service-modal")));
        select.selectByIndex(1);
        waitOperador.until(ExpectedConditions.elementToBeClickable(By.id("btn-confirm-add-service"))).click();
        waitOperador.until(d -> d.findElements(By.className("service-account-item")).size() == cantidadInicial + 2);

        BigDecimal esperado = montoInicial.add(precio1).add(precio2);
        BigDecimal total = dinero(driverOperador.findElement(By.id("modal-total-account-price")).getText());
        Assertions.assertThat(total).isEqualByComparingTo(esperado);
        Assertions.assertThat(driverOperador.findElements(By.className("service-account-item")))
                .hasSize(cantidadInicial + 2);
        waitOperador.until(ExpectedConditions.elementToBeClickable(By.id("btn-close-service-modal"))).click();

        // 5. El operador paga la cuenta de esa misma reserva.
        clickFila(idReserva, "btn-pagar-cuenta");
        String descripcion = waitOperador.until(ExpectedConditions.visibilityOfElementLocated(
                By.id("modal-description"))).getText();
        Assertions.assertThat(descripcion).contains("#" + idReserva);
        Assertions.assertThat(dinero(descripcion.split("para la reserva")[0]))
                .isEqualByComparingTo(esperado);
        confirmarModal();
        waitOperador.until(d -> !fila(idReserva).findElements(By.className("cuenta-pagada-badge")).isEmpty());

        // 6. Checkout: la misma reserva debe pasar a INACTIVA.
        clickFila(idReserva, "btn-finalizar-estadia");
        confirmarModal();
        // Esperar el cambio del estado de ESA reserva, sin depender de mayúsculas.
        try {
            waitOperador.until(d -> {
                List<WebElement> estados = d.findElements(By.cssSelector(
                        "tr.reservation-table-row[data-id='" + idReserva + "'] .res-row-status"));
                return !estados.isEmpty()
                        && estados.get(0).getText().trim().equalsIgnoreCase("Inactiva");
            });
        } catch (org.openqa.selenium.TimeoutException ex) {
            String estadoActual = fila(idReserva).findElement(By.className("res-row-status")).getText();
            throw new AssertionError("La reserva #" + idReserva
                    + " no cambió a INACTIVA en la vista del operador. Estado mostrado: "
                    + estadoActual, ex);
        }
        Assertions.assertThat(fila(idReserva).findElement(By.className("res-row-status"))
                .getText()).containsIgnoringCase("Inactiva");

        // 7. El usuario comprueba la finalización y el pago.
        driverUsuario.navigate().refresh();
        waitUsuario.until(ExpectedConditions.numberOfElementsToBeMoreThan(
                By.className("reservation-card-item"), 0));
        waitUsuario.until(d -> {
            WebElement tarjeta = tarjetaUsuario(idReserva);
            return tarjeta.findElement(By.className("res-card-status")).getText()
                    .toLowerCase(java.util.Locale.ROOT).contains("finalizada")
                    && tarjeta.findElement(By.className("res-card-paid-badge")).getText()
                    .toLowerCase(java.util.Locale.ROOT).contains("pagada");
        });
        WebElement tarjetaFinal = tarjetaUsuario(idReserva);
        Assertions.assertThat(tarjetaFinal.findElement(By.className("res-card-status")).getText())
                .containsIgnoringCase("Finalizada");
        Assertions.assertThat(tarjetaFinal.findElement(By.className("res-card-paid-badge")).getText())
                .containsIgnoringCase("Pagada");
    }
}

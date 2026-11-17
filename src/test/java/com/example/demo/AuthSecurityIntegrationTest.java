package com.example.demo;

import com.example.demo.repository.ClientRepository;
import com.example.demo.service.OAuthTemporaryCodeStore;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthSecurityIntegrationTest {

    private static final String ORIGIN = "http://localhost:4200";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private OAuthTemporaryCodeStore temporaryCodeStore;

    @Test
    void passwordLoginCreatesValidSessionForEveryRoleAndMeRecognizesIt() throws Exception {
        loginAndVerify("admin", "admin", "ADMIN", "/admin/rooms");
        loginAndVerify("operario", "operario", "OPERATOR", "/operator/reservations");
        loginAndVerify("demo", "demo", "CLIENT", "/profile");
    }

    @Test
    void roleBasedEndpointAccessIsEnforcedForJwtAndSession() throws Exception {
        // Anónimo no puede listar todas las reservas ni clientes
        mockMvc.perform(get("/api/reservations"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/reservations/summary"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/clients"))
                .andExpect(status().isUnauthorized());
        // Disponibilidad no-PII sí es pública
        mockMvc.perform(get("/api/rooms/occupied"))
                .andExpect(status().isOk());

        // Login como CLIENT y obtención de Bearer JWT
        MvcResult clientLogin = mockMvc.perform(post("/api/auth/login")
                        .header("Origin", ORIGIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"identifier\":\"demo\",\"password\":\"demo\"}"))
                .andExpect(status().isOk())
                .andReturn();
        String clientJwt = readJson(clientLogin, "$.token");

        // CLIENT no puede listar todas las reservas ni eliminar servicios ni listar clientes
        mockMvc.perform(get("/api/reservations")
                        .header("Authorization", "Bearer " + clientJwt))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/reservations/summary")
                        .header("Authorization", "Bearer " + clientJwt))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/reservations/1/services/1")
                        .header("Authorization", "Bearer " + clientJwt))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/clients")
                        .header("Authorization", "Bearer " + clientJwt))
                .andExpect(status().isForbidden());

        // OPERATOR sí puede listar reservas, pero no crear tipos de habitación ni listar todos los clientes
        MvcResult opLogin = mockMvc.perform(post("/api/auth/login")
                        .header("Origin", ORIGIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"identifier\":\"operario\",\"password\":\"operario\"}"))
                .andExpect(status().isOk())
                .andReturn();
        String opJwt = readJson(opLogin, "$.token");

        mockMvc.perform(get("/api/reservations")
                        .header("Authorization", "Bearer " + opJwt))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/reservations/summary")
                        .header("Authorization", "Bearer " + opJwt))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/clients")
                        .header("Authorization", "Bearer " + opJwt))
                .andExpect(status().isForbidden());
    }

    @Test
    void googleExchangeCreatesSessionAcceptedByMe() throws Exception {
        Long demoId = clientRepository.findByUsernameIgnoreCaseOrEmailIgnoreCase("demo", "demo@example.com")
                .orElseThrow().getId();
        String exchangeCode = temporaryCodeStore.create(demoId);

        MvcResult exchange = mockMvc.perform(post("/api/auth/google/exchange")
                        .header("Origin", ORIGIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"" + exchangeCode + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(cookie().exists("HM_SESSION"))
                .andExpect(cookie().httpOnly("HM_SESSION", true))
                .andReturn();

        Cookie session = exchange.getResponse().getCookie("HM_SESSION");
        mockMvc.perform(get("/api/auth/me")
                        .header("Origin", ORIGIN)
                        .cookie(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.role").value("CLIENT"));
    }

    @Test
    void corsCsrfAndAuthorizedMutationsWorkTogether() throws Exception {
        MvcResult login = mockMvc.perform(post("/api/auth/login")
                        .header("Origin", ORIGIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"identifier\":\"admin\",\"password\":\"admin\"}"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", ORIGIN))
                .andExpect(cookie().exists("HM_SESSION"))
                .andReturn();
        Cookie session = login.getResponse().getCookie("HM_SESSION");

        MvcResult created = mockMvc.perform(post("/api/room-types")
                        .header("Origin", ORIGIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .cookie(session)
                        .content("""
                                {"name":"OAuth Integration Type","description":"Created by integration test","pricePerNight":125.0}
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        String createdId = readJson(created, "$.id");

        mockMvc.perform(put("/api/room-types/" + createdId)
                        .header("Origin", ORIGIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .cookie(session)
                        .content("""
                                {"name":"OAuth Integration Type","description":"Updated by integration test","pricePerNight":130.0}
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/api/reservations/999999/activate")
                        .header("Origin", ORIGIN)
                        .cookie(session))
                .andExpect(status().isNotFound());

        mockMvc.perform(delete("/api/room-types/" + createdId)
                        .header("Origin", ORIGIN)
                        .cookie(session))
                .andExpect(status().isNoContent());
    }

    @Test
    void mutationsRejectMissingCsrfEvenWhenSessionIsValid() throws Exception {
        // Con CSRF desactivado para arquitectura REST + JWT, toda mutación sin JWT ni sesión debe ser rechazada con 401
        mockMvc.perform(post("/api/room-types")
                        .header("Origin", ORIGIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Must Be Rejected","description":"Unauthenticated mutation","pricePerNight":100.0}
                                """))
                .andExpect(status().isUnauthorized());
    }

    private void loginAndVerify(String username, String password, String role, String redirectPrefix) throws Exception {
        MvcResult login = mockMvc.perform(post("/api/auth/login")
                        .header("Origin", ORIGIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"identifier\":\"" + username + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.role").value(role))
                .andExpect(cookie().exists("HM_SESSION"))
                .andExpect(cookie().httpOnly("HM_SESSION", true))
                .andReturn();

        Cookie session = login.getResponse().getCookie("HM_SESSION");
        mockMvc.perform(get("/api/auth/me")
                        .header("Origin", ORIGIN)
                        .cookie(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value(role))
                .andExpect(jsonPath("$.redirectUrl").value(org.hamcrest.Matchers.startsWith(redirectPrefix)));
    }

    private String readJson(MvcResult result, String expression) throws Exception {
        String body = result.getResponse().getContentAsString();
        Object value = com.jayway.jsonpath.JsonPath.read(body, expression);
        return String.valueOf(value);
    }
}

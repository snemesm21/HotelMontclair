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
        loginAndVerify("demo", "demo", "CLIENT", "/profile/");
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
        MvcResult csrf = mockMvc.perform(get("/api/auth/csrf").header("Origin", ORIGIN))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", ORIGIN))
                .andExpect(jsonPath("$.headerName").value("X-XSRF-TOKEN"))
                .andExpect(cookie().exists("XSRF-TOKEN"))
                .andReturn();
        Cookie csrfCookie = csrf.getResponse().getCookie("XSRF-TOKEN");
        String csrfToken = readJson(csrf, "$.token");

        MvcResult login = mockMvc.perform(post("/api/auth/login")
                        .header("Origin", ORIGIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"identifier\":\"admin\",\"password\":\"admin\"}")
                        .cookie(csrfCookie))
                .andExpect(status().isOk())
                .andExpect(cookie().exists("HM_SESSION"))
                .andReturn();
        Cookie session = login.getResponse().getCookie("HM_SESSION");

        MvcResult created = mockMvc.perform(post("/api/room-types")
                        .header("Origin", ORIGIN)
                        .header("X-XSRF-TOKEN", csrfToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .cookie(session, csrfCookie)
                        .content("""
                                {"name":"OAuth Integration Type","description":"Created by integration test","pricePerNight":125.0}
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        String createdId = readJson(created, "$.id");

        mockMvc.perform(put("/api/room-types/" + createdId)
                        .header("Origin", ORIGIN)
                        .header("X-XSRF-TOKEN", csrfToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .cookie(session, csrfCookie)
                        .content("""
                                {"name":"OAuth Integration Type","description":"Updated by integration test","pricePerNight":130.0}
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/api/reservations/999999/activate")
                        .header("Origin", ORIGIN)
                        .header("X-XSRF-TOKEN", csrfToken)
                        .cookie(session, csrfCookie))
                .andExpect(status().isNotFound());

        mockMvc.perform(delete("/api/room-types/" + createdId)
                        .header("Origin", ORIGIN)
                        .header("X-XSRF-TOKEN", csrfToken)
                        .cookie(session, csrfCookie))
                .andExpect(status().isNoContent());
    }

    @Test
    void mutationsRejectMissingCsrfEvenWhenSessionIsValid() throws Exception {
        MvcResult login = mockMvc.perform(post("/api/auth/login")
                        .header("Origin", ORIGIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"identifier\":\"admin\",\"password\":\"admin\"}"))
                .andExpect(status().isOk())
                .andReturn();
        Cookie session = login.getResponse().getCookie("HM_SESSION");

        mockMvc.perform(post("/api/room-types")
                        .header("Origin", ORIGIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .cookie(session)
                        .content("""
                                {"name":"Must Be Rejected","description":"Missing CSRF","pricePerNight":100.0}
                                """))
                .andExpect(status().isForbidden());
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

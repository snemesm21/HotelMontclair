package com.example.demo;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.demo.entities.TouristPlace;
import com.example.demo.repository.ItineraryStopRepository;
import com.example.demo.repository.TouristPlaceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TourismSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TouristPlaceRepository placeRepository;

    @Autowired
    private ItineraryStopRepository itineraryRepository;

    private TouristPlace destination;

    @BeforeEach
    void setUp() {
        itineraryRepository.deleteAll();
        destination = placeRepository.findBySlugAndActiveTrue("casino-de-monte-carlo").orElseThrow();
    }

    @Test
    void catalogIsPublicButItineraryRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/tourism/places"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.slug == 'casino-de-monte-carlo')]").exists());
        mockMvc.perform(get("/api/tourism/places/museo-oceanografico"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Museo Oceanográfico de Mónaco"));
        mockMvc.perform(get("/api/tourism/itinerary"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/tourism/itinerary/optimize")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/tourism/itinerary/optimize")
                        .header("Authorization", "Bearer " + token("demo", "demo"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void itineraryIsPersistedDeduplicatedAndScopedToAuthenticatedClient() throws Exception {
        String guestToken = token("demo", "demo");
        String otherToken = token("admin", "admin");

        mockMvc.perform(post("/api/tourism/itinerary/" + destination.getId())
                        .header("Authorization", "Bearer " + guestToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.place.id").value(destination.getId()));
        mockMvc.perform(post("/api/tourism/itinerary/" + destination.getId())
                        .header("Authorization", "Bearer " + guestToken))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/tourism/itinerary")
                        .header("Authorization", "Bearer " + guestToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
        mockMvc.perform(get("/api/tourism/itinerary")
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
        mockMvc.perform(post("/api/tourism/itinerary/" + destination.getId())
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isOk());
        mockMvc.perform(delete("/api/tourism/itinerary/" + destination.getId())
                        .header("Authorization", "Bearer " + guestToken))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/tourism/itinerary")
                        .header("Authorization", "Bearer " + guestToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
        mockMvc.perform(get("/api/tourism/itinerary")
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void catalogCanOnlyBeExtendedByAdmin() throws Exception {
        String request = """
                {
                  "slug":"test-monaco-destination",
                  "name":"Test destination",
                  "description":"Test description",
                  "category":"CULTURA",
                  "imageUrls":["https://example.com/image.jpg"],
                  "address":"Monaco",
                  "latitude":43.73,
                  "longitude":7.42,
                  "suggestedDurationMinutes":30
                }
                """;
        mockMvc.perform(post("/api/tourism/places")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/tourism/places")
                        .header("Authorization", "Bearer " + token("demo", "demo"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/tourism/places")
                        .header("Authorization", "Bearer " + token("admin", "admin"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.slug").value("test-monaco-destination"));
        mockMvc.perform(get("/api/tourism/places/test-monaco-destination"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Test destination"));
    }

    private String token(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"identifier\":\"" + username + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        return com.jayway.jsonpath.JsonPath.read(result.getResponse().getContentAsString(), "$.token");
    }
}

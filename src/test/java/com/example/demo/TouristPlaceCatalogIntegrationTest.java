package com.example.demo;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.demo.entities.TouristPlace;
import com.example.demo.repository.TouristPlaceRepository;
import com.jayway.jsonpath.JsonPath;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
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
class TouristPlaceCatalogIntegrationTest {

    private static final String HIDDEN_PLACE_SLUG = "inactive-catalog-test-place";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TouristPlaceRepository placeRepository;

    @AfterEach
    void cleanUp() {
        placeRepository.findBySlugAndActiveTrue(HIDDEN_PLACE_SLUG)
                .ifPresent(placeRepository::delete);
        placeRepository.findAll().stream()
                .filter(place -> HIDDEN_PLACE_SLUG.equals(place.getSlug()))
                .forEach(placeRepository::delete);
    }

    @Test
    void publicCatalogReturnsRequiredPlacesInDisplayOrderWithLicensedImages() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/tourist-places"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(greaterThanOrEqualTo(12)))
                .andReturn();

        String catalog = result.getResponse().getContentAsString();
        List<String> slugs = JsonPath.read(catalog, "$[*].slug");
        int casinoIndex = slugs.indexOf("casino-de-monte-carlo");
        int portIndex = slugs.indexOf("puerto-hercules");
        int palaceIndex = slugs.indexOf("palacio-del-principe");
        int museumIndex = slugs.indexOf("museo-oceanografico");
        assertTrue(casinoIndex >= 0 && casinoIndex < portIndex);
        assertTrue(portIndex < palaceIndex && palaceIndex < museumIndex);

        mockMvc.perform(get("/api/tourist-places").param("category", "cultura"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.slug == 'casino-de-monte-carlo')]").exists());
        mockMvc.perform(get("/api/tourist-places/{id}",
                        placeRepository.findBySlugAndActiveTrue("casino-de-monte-carlo").orElseThrow().getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shortDescription").isNotEmpty())
                .andExpect(jsonPath("$.longDescription").value(containsString("Belle Époque")))
                .andExpect(jsonPath("$.displayOrder").value(1))
                .andExpect(jsonPath("$.imageUrls[0]").value(containsString("wikimedia.org")))
                .andExpect(jsonPath("$.imageAttributions[0]").value(containsString("CC BY-SA 4.0")));
    }

    @Test
    void publicCatalogSupportsCategoryFilterAndActivePlaceLookupById() throws Exception {
        TouristPlace casino = placeRepository.findBySlugAndActiveTrue("casino-de-monte-carlo").orElseThrow();

        mockMvc.perform(get("/api/tourist-places").param("category", "cultura"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].slug").value("casino-de-monte-carlo"));

        mockMvc.perform(get("/api/tourist-places/{id}", casino.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(casino.getId()))
                .andExpect(jsonPath("$.longDescription").value(casino.getDescription()));

        TouristPlace inactive = new TouristPlace();
        inactive.setSlug(HIDDEN_PLACE_SLUG);
        inactive.setName("Inactive test place");
        inactive.setDescription("Long test description");
        inactive.setShortDescription("Short test description");
        inactive.setCategory("TEST");
        inactive.setAddress("Monaco");
        inactive.setLatitude(43.7);
        inactive.setLongitude(7.4);
        inactive.setSuggestedDurationMinutes(30);
        inactive.setDisplayOrder(0);
        inactive.setActive(false);
        inactive = placeRepository.saveAndFlush(inactive);

        mockMvc.perform(get("/api/tourist-places"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.slug == 'inactive-catalog-test-place')]").doesNotExist());
        mockMvc.perform(get("/api/tourist-places/{id}", inactive.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    void visitorsCannotCreateUpdateOrDeleteTouristPlaces() throws Exception {
        long placeCount = placeRepository.count();
        mockMvc.perform(post("/api/tourist-places")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(put("/api/tourist-places/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(delete("/api/tourist-places/1"))
                .andExpect(status().isUnauthorized());

        String guestToken = token("demo", "demo");
        mockMvc.perform(post("/api/tourist-places")
                        .header("Authorization", "Bearer " + guestToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isMethodNotAllowed());
        mockMvc.perform(put("/api/tourist-places/1")
                        .header("Authorization", "Bearer " + guestToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isMethodNotAllowed());
        mockMvc.perform(delete("/api/tourist-places/1")
                        .header("Authorization", "Bearer " + guestToken))
                .andExpect(status().isMethodNotAllowed());
        org.junit.jupiter.api.Assertions.assertEquals(placeCount, placeRepository.count());
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

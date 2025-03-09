package com.olprog.yahourt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.olprog.yahourt.dto.OptimizationRequestDto;
import com.olprog.yahourt.model.SimulationResult;

@SpringBootTest
@AutoConfigureMockMvc
public class YogurtOptimizerIntegrationTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @Test
  @DisplayName("Test d'intégration: Devrait récupérer les paramètres par défaut")
  void shouldGetDefaultParams() throws Exception {
    MvcResult result = mockMvc.perform(get("/api/yogurt/default-params"))
        .andExpect(status().isOk())
        .andReturn();

    String content = result.getResponse().getContentAsString();
    assertNotNull(content);
    assertTrue(content.contains("initialStock"));
    assertTrue(content.contains("deliveryDelay"));
    assertTrue(content.contains("packSize"));
    assertTrue(content.contains("purchaseDay"));
    assertTrue(content.contains("consumptionProfile"));
  }

  @Test
  @DisplayName("Test d'intégration: Devrait optimiser les stocks avec paramètres par défaut")
  void shouldOptimizeWithDefaultParams() throws Exception {
    MvcResult result = mockMvc.perform(post("/api/yogurt/optimize")
        .contentType(MediaType.APPLICATION_JSON)
        .content("{}"))
        .andExpect(status().isOk())
        .andReturn();

    String content = result.getResponse().getContentAsString();
    SimulationResult simulationResult = objectMapper.readValue(content, SimulationResult.class);

    assertNotNull(simulationResult);
    assertNotNull(simulationResult.getPurchaseRecommendations());
    assertNotNull(simulationResult.getDailyStockLevels());
    assertNotNull(simulationResult.getSummary());

    // Vérification que la simulation couvre une année
    assertEquals(365, simulationResult.getDailyStockLevels().size());
  }

  @Test
  @DisplayName("Test d'intégration: Devrait optimiser les stocks avec paramètres personnalisés")
  void shouldOptimizeWithCustomParams() throws Exception {
    // Préparation de la requête personnalisée
    OptimizationRequestDto requestDto = new OptimizationRequestDto();
    requestDto.setInitialStock(15);
    requestDto.setDeliveryDelay(1);
    requestDto.setPackSize(6);

    Map<String, Integer> customConsumption = new HashMap<>();
    customConsumption.put("MONDAY", 6);
    customConsumption.put("TUESDAY", 6);
    customConsumption.put("WEDNESDAY", 6);
    customConsumption.put("THURSDAY", 6);
    customConsumption.put("FRIDAY", 6);
    customConsumption.put("SATURDAY", 8);
    customConsumption.put("SUNDAY", 8);
    requestDto.setDailyConsumption(customConsumption);

    MvcResult result = mockMvc.perform(post("/api/yogurt/optimize")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(requestDto)))
        .andExpect(status().isOk())
        .andReturn();

    String content = result.getResponse().getContentAsString();
    SimulationResult simulationResult = objectMapper.readValue(content, SimulationResult.class);

    assertNotNull(simulationResult);
    // Avec une consommation plus élevée, on s'attend à plus d'achats
    assertTrue(simulationResult.getPurchaseRecommendations().size() > 0);

    // Le stock ne doit jamais être négatif
    simulationResult.getDailyStockLevels()
        .forEach(level -> assertTrue(level.getStockLevel() >= 0, "Le stock ne devrait jamais être négatif"));
  }

  @Test
  @DisplayName("Test d'intégration: Devrait rejeter des paramètres invalides")
  void shouldRejectInvalidParams() throws Exception {
    // Stock initial négatif
    OptimizationRequestDto invalidDto = new OptimizationRequestDto();
    invalidDto.setInitialStock(-5);

    mockMvc.perform(post("/api/yogurt/optimize")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(invalidDto)))
        .andExpect(status().isBadRequest());

    // Délai de livraison invalide
    invalidDto = new OptimizationRequestDto();
    invalidDto.setDeliveryDelay(0);

    mockMvc.perform(post("/api/yogurt/optimize")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(invalidDto)))
        .andExpect(status().isBadRequest());

    // Taille de paquet invalide
    invalidDto = new OptimizationRequestDto();
    invalidDto.setPackSize(0);

    mockMvc.perform(post("/api/yogurt/optimize")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(invalidDto)))
        .andExpect(status().isBadRequest());
  }
}

package com.olprog.yahourt.controller;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.olprog.yahourt.dto.OptimizationRequestDto;
import com.olprog.yahourt.model.SimulationResult;
import com.olprog.yahourt.model.StockSimulationParams;
import com.olprog.yahourt.service.YogurtOptimizationService;

@WebMvcTest(YogurtOptimizerController.class)
public class YogurtOptimizerControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private YogurtOptimizationService optimizationService;

  @Autowired
  private ObjectMapper objectMapper;

  private StockSimulationParams defaultParams;
  private SimulationResult mockResult;

  @BeforeEach
  void setUp() {
    defaultParams = StockSimulationParams.createDefault();

    // Création d'un SimulationResult minimal pour les tests
    mockResult = SimulationResult.builder()
        .purchaseRecommendations(Collections.emptyList())
        .dailyStockLevels(Collections.emptyList())
        .summary(null)
        .build();

    // Configuration du mock service
    when(optimizationService.convertRequestToParams(any())).thenReturn(defaultParams);
    when(optimizationService.simulateYogurtStockForYear(any())).thenReturn(mockResult);
  }

  @Test
  @DisplayName("Devrait retourner les paramètres par défaut")
  void shouldReturnDefaultParameters() throws Exception {
    mockMvc.perform(get("/api/yogurt/default-params"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.initialStock", is(6)))
        .andExpect(jsonPath("$.deliveryDelay", is(2)))
        .andExpect(jsonPath("$.packSize", is(2)))
        .andExpect(jsonPath("$.purchaseDay", is("SUNDAY")));
  }

  @Test
  @DisplayName("Devrait optimiser les stocks avec requête vide")
  void shouldOptimizeStockWithEmptyRequest() throws Exception {
    mockMvc.perform(post("/api/yogurt/optimize")
        .contentType(MediaType.APPLICATION_JSON)
        .content("{}"))
        .andExpect(status().isOk());

    verify(optimizationService).convertRequestToParams(any());
    verify(optimizationService).simulateYogurtStockForYear(any());
  }

  @Test
  @DisplayName("Devrait optimiser les stocks avec paramètres personnalisés")
  void shouldOptimizeStockWithCustomParameters() throws Exception {
    // Préparation de la requête personnalisée
    OptimizationRequestDto requestDto = new OptimizationRequestDto();
    requestDto.setInitialStock(10);
    requestDto.setDeliveryDelay(3);
    requestDto.setPackSize(4);

    Map<String, Integer> customConsumption = new HashMap<>();
    customConsumption.put("MONDAY", 4);
    customConsumption.put("FRIDAY", 5);
    requestDto.setDailyConsumption(customConsumption);

    mockMvc.perform(post("/api/yogurt/optimize")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(requestDto)))
        .andExpect(status().isOk());

    verify(optimizationService).convertRequestToParams(any());
    verify(optimizationService).simulateYogurtStockForYear(any());
  }

  @Test
  @DisplayName("Devrait rejeter une requête avec paramètres invalides")
  void shouldRejectRequestWithInvalidParameters() throws Exception {
    // Préparation d'une requête avec des paramètres invalides
    OptimizationRequestDto requestDto = new OptimizationRequestDto();
    requestDto.setInitialStock(-1); // Stock initial négatif

    mockMvc.perform(post("/api/yogurt/optimize")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(requestDto)))
        .andExpect(status().isBadRequest());
  }
}

package com.olprog.yahourt.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import com.olprog.yahourt.dto.OptimizationRequestDto;
import com.olprog.yahourt.exception.ValidationException;
import com.olprog.yahourt.model.ConsumptionProfile;
import com.olprog.yahourt.model.DailyStockLevel;
import com.olprog.yahourt.model.PurchaseRecommendation;
import com.olprog.yahourt.model.SimulationResult;
import com.olprog.yahourt.model.SimulationSummary;
import com.olprog.yahourt.model.StockSimulationParams;

@ExtendWith(MockitoExtension.class)
public class YogurtOptimizationServiceTest {

    @InjectMocks
    private YogurtOptimizationService optimizationService;

    private StockSimulationParams defaultParams;

    @BeforeEach
    void setUp() {
        defaultParams = StockSimulationParams.createDefault();
    }

    @Test
    @DisplayName("Devrait convertir DTO avec valeurs par défaut quand DTO est null")
    void shouldConvertNullDtoToDefaultParams() {
        // When
        StockSimulationParams result = optimizationService.convertRequestToParams(null);

        // Then
        assertEquals(defaultParams.getInitialStock(), result.getInitialStock());
        assertEquals(defaultParams.getDeliveryDelay(), result.getDeliveryDelay());
        assertEquals(defaultParams.getPackSize(), result.getPackSize());
        assertEquals(defaultParams.getPurchaseDay(), result.getPurchaseDay());
        assertEquals(defaultParams.getStartDate(), result.getStartDate());

        // Vérification du profil de consommation par défaut
        for (DayOfWeek day : DayOfWeek.values()) {
            assertEquals(
                    defaultParams.getConsumptionProfile().getConsumptionForDay(day),
                    result.getConsumptionProfile().getConsumptionForDay(day));
        }
    }

    @Test
    @DisplayName("Devrait convertir DTO avec valeurs personnalisées")
    void shouldConvertCustomDtoToParams() {
        // Given
        OptimizationRequestDto dto = new OptimizationRequestDto();
        dto.setInitialStock(10);
        dto.setDeliveryDelay(3);
        dto.setPackSize(4);

        Map<String, Integer> customConsumption = new HashMap<>();
        customConsumption.put("MONDAY", 4);
        customConsumption.put("FRIDAY", 5);
        dto.setDailyConsumption(customConsumption);

        // When
        StockSimulationParams result = optimizationService.convertRequestToParams(dto);

        // Then
        assertEquals(10, result.getInitialStock());
        assertEquals(3, result.getDeliveryDelay());
        assertEquals(4, result.getPackSize());
        assertEquals(defaultParams.getPurchaseDay(), result.getPurchaseDay());
        assertEquals(defaultParams.getStartDate(), result.getStartDate());

        // Vérification des jours personnalisés
        assertEquals(4, result.getConsumptionProfile().getConsumptionForDay(DayOfWeek.MONDAY));
        assertEquals(5, result.getConsumptionProfile().getConsumptionForDay(DayOfWeek.FRIDAY));

        // Vérification que les autres jours conservent les valeurs par défaut
        assertEquals(defaultParams.getConsumptionProfile().getConsumptionForDay(DayOfWeek.TUESDAY),
                result.getConsumptionProfile().getConsumptionForDay(DayOfWeek.TUESDAY));
    }

    @Test
    @DisplayName("Devrait lever une exception pour un jour de la semaine invalide")
    void shouldThrowExceptionForInvalidDayOfWeek() {
        // Given
        OptimizationRequestDto dto = new OptimizationRequestDto();
        Map<String, Integer> customConsumption = new HashMap<>();
        customConsumption.put("INVALID_DAY", 4);
        dto.setDailyConsumption(customConsumption);

        // When & Then
        assertThrows(ValidationException.class, () -> {
            optimizationService.convertRequestToParams(dto);
        });
    }

    @Test
    @DisplayName("Devrait simuler le stock correctement avec les paramètres par défaut")
    void shouldSimulateStockCorrectlyWithDefaultParams() {
        // When
        SimulationResult result = optimizationService.simulateYogurtStockForYear(defaultParams);

        // Then
        assertNotNull(result);
        assertNotNull(result.getPurchaseRecommendations());
        assertNotNull(result.getDailyStockLevels());
        assertNotNull(result.getSummary());

        // Vérification que la simulation couvre une année
        assertEquals(365, result.getDailyStockLevels().size());

        // Vérification que la synthèse est correcte
        SimulationSummary summary = result.getSummary();
        assertTrue(summary.getTotalPurchases() > 0);
        assertTrue(summary.getTotalYogurtsPurchased() > 0);
        assertTrue(summary.getTotalYogurtsConsumed() > 0);

        // La consommation totale devrait être cohérente avec le profil de consommation
        // int expectedYearlyConsumption =
        // calculateExpectedYearlyConsumption(defaultParams);
        // assertEquals(expectedYearlyConsumption, summary.getTotalYogurtsConsumed());
    }

    private int calculateExpectedYearlyConsumption(StockSimulationParams params) {
        // Cette méthode calcule la consommation attendue sur un an en fonction du
        // profil
        LocalDate startDate = params.getStartDate();
        LocalDate endDate = startDate.plusDays(365);
        int totalConsumption = 0;

        LocalDate currentDate = startDate;
        while (currentDate.isBefore(endDate)) {
            DayOfWeek dayOfWeek = currentDate.getDayOfWeek();
            totalConsumption += params.getConsumptionProfile().getConsumptionForDay(dayOfWeek);
            currentDate = currentDate.plusDays(1);
        }

        return totalConsumption;
    }

    @Test
    @DisplayName("Devrait valider que le stock ne devient jamais négatif")
    void shouldEnsureStockNeverBecomesNegative() {
        // When
        SimulationResult result = optimizationService.simulateYogurtStockForYear(defaultParams);

        // Then
        for (DailyStockLevel level : result.getDailyStockLevels()) {
            assertTrue(level.getStockLevel() >= 0,
                    "Le stock ne devrait jamais être négatif, mais était " +
                            level.getStockLevel() + " le " + level.getDate());
        }
    }

    @Test
    @DisplayName("Devrait valider que les livraisons arrivent le bon jour")
    void shouldEnsureDeliveriesArriveOnCorrectDay() {
        // When
        SimulationResult result = optimizationService.simulateYogurtStockForYear(defaultParams);

        // Then
        for (PurchaseRecommendation purchase : result.getPurchaseRecommendations()) {
            LocalDate orderDate = purchase.getOrderDate();
            LocalDate expectedDeliveryDate = orderDate.plusDays(defaultParams.getDeliveryDelay());
            assertEquals(expectedDeliveryDate, purchase.getDeliveryDate(),
                    "La date de livraison devrait être " + expectedDeliveryDate +
                            " mais était " + purchase.getDeliveryDate());
        }
    }

    @Test
    @DisplayName("Devrait valider que les achats se font le jour désigné")
    void shouldEnsurePurchasesOccurOnDesignatedDay() {
        // When
        SimulationResult result = optimizationService.simulateYogurtStockForYear(defaultParams);

        // Then
        for (PurchaseRecommendation purchase : result.getPurchaseRecommendations()) {
            assertEquals(defaultParams.getPurchaseDay(), purchase.getOrderDate().getDayOfWeek(),
                    "Les achats devraient se faire le " + defaultParams.getPurchaseDay() +
                            " mais un achat a été effectué le " + purchase.getOrderDate().getDayOfWeek());
        }
    }

    @Test
    @DisplayName("Devrait lever une exception pour des paramètres invalides")
    void shouldThrowExceptionForInvalidParameters() {
        // Test avec stock initial négatif
        final StockSimulationParams invalidParamsNegativeStock = StockSimulationParams.builder()
                .startDate(LocalDate.now())
                .initialStock(-1)
                .deliveryDelay(2)
                .packSize(2)
                .purchaseDay(DayOfWeek.SUNDAY)
                .consumptionProfile(ConsumptionProfile.createDefault())
                .build();

        assertThrows(ValidationException.class, () -> {
            optimizationService.simulateYogurtStockForYear(invalidParamsNegativeStock);
        });

        // Test avec délai de livraison invalide
        final StockSimulationParams invalidParamsInvalidDelay = StockSimulationParams.builder()
                .startDate(LocalDate.now())
                .initialStock(5)
                .deliveryDelay(0)
                .packSize(2)
                .purchaseDay(DayOfWeek.SUNDAY)
                .consumptionProfile(ConsumptionProfile.createDefault())
                .build();

        assertThrows(ValidationException.class, () -> {
            optimizationService.simulateYogurtStockForYear(invalidParamsInvalidDelay);
        });

        // Test avec taille de paquet invalide
        final StockSimulationParams invalidParamsInvalidPackSize = StockSimulationParams.builder()
                .startDate(LocalDate.now())
                .initialStock(5)
                .deliveryDelay(2)
                .packSize(0)
                .purchaseDay(DayOfWeek.SUNDAY)
                .consumptionProfile(ConsumptionProfile.createDefault())
                .build();

        assertThrows(ValidationException.class, () -> {
            optimizationService.simulateYogurtStockForYear(invalidParamsInvalidPackSize);
        });

        // Test avec profil de consommation null
        final StockSimulationParams invalidParamsNullProfile = StockSimulationParams.builder()
                .startDate(LocalDate.now())
                .initialStock(5)
                .deliveryDelay(2)
                .packSize(2)
                .purchaseDay(DayOfWeek.SUNDAY)
                .consumptionProfile(null)
                .build();

        assertThrows(ValidationException.class, () -> {
            optimizationService.simulateYogurtStockForYear(invalidParamsNullProfile);
        });
    }

    @ParameterizedTest
    @MethodSource("provideCustomConsumptionProfiles")
    @DisplayName("Devrait simuler correctement avec différents profils de consommation")
    void shouldSimulateCorrectlyWithDifferentConsumptionProfiles(Map<DayOfWeek, Integer> consumptionMap,
            int expectedWeeklyConsumption) {
        // Given
        ConsumptionProfile customProfile = ConsumptionProfile.builder()
                .dailyConsumption(consumptionMap)
                .build();

        StockSimulationParams customParams = StockSimulationParams.builder()
                .startDate(LocalDate.of(2025, 1, 5))
                .initialStock(6)
                .deliveryDelay(2)
                .packSize(2)
                .purchaseDay(DayOfWeek.SUNDAY)
                .consumptionProfile(customProfile)
                .build();

        // When
        SimulationResult result = optimizationService.simulateYogurtStockForYear(customParams);

        // Then
        // Vérification de base que la simulation a fonctionné
        assertNotNull(result);

        // Vérification que la consommation est correcte
        int expectedYearlyConsumption = calculateExpectedYearlyConsumption(customParams);
        assertEquals(expectedYearlyConsumption, result.getSummary().getTotalYogurtsConsumed());

        // Vérification approximative basée sur la consommation hebdomadaire
        // 52 semaines + quelques jours dans une année
        assertTrue(Math.abs(
                expectedWeeklyConsumption * 52
                        - result.getSummary().getTotalYogurtsConsumed()) < expectedWeeklyConsumption);
    }

    static Stream<Arguments> provideCustomConsumptionProfiles() {
        // Profil avec consommation uniforme
        Map<DayOfWeek, Integer> uniformProfile = new HashMap<>();
        for (DayOfWeek day : DayOfWeek.values()) {
            uniformProfile.put(day, 2);
        }

        // Profil avec week-end chargé
        Map<DayOfWeek, Integer> weekendHeavyProfile = new HashMap<>();
        for (DayOfWeek day : DayOfWeek.values()) {
            if (day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY) {
                weekendHeavyProfile.put(day, 5);
            } else {
                weekendHeavyProfile.put(day, 1);
            }
        }

        // Profil avec semaine chargée
        Map<DayOfWeek, Integer> weekdayHeavyProfile = new HashMap<>();
        for (DayOfWeek day : DayOfWeek.values()) {
            if (day != DayOfWeek.SATURDAY && day != DayOfWeek.SUNDAY) {
                weekdayHeavyProfile.put(day, 4);
            } else {
                weekdayHeavyProfile.put(day, 1);
            }
        }

        return Stream.of(
                Arguments.of(uniformProfile, 14), // 2 * 7 = 14 par semaine
                Arguments.of(weekendHeavyProfile, 15), // (5*2) + (1*5) = 15 par semaine
                Arguments.of(weekdayHeavyProfile, 22) // (4*5) + (1*2) = 22 par semaine
        );
    }

    @Test
    @DisplayName("Devrait optimiser les achats pour minimiser les ruptures de stock")
    void shouldOptimizePurchasesToMinimizeStockouts() {
        // When
        SimulationResult result = optimizationService.simulateYogurtStockForYear(defaultParams);

        // Then
        // Un stock minimum de 0 indiquerait au moins une rupture de stock
        assertTrue(result.getSummary().getMinimumStockLevel() >= 0,
                "Le stock minimum ne devrait pas être négatif");

        // La stratégie d'optimisation devrait permettre d'éviter les ruptures de stock
        // en commandant suffisamment à l'avance
        for (DailyStockLevel level : result.getDailyStockLevels()) {
            // Si c'est un jour d'achat avec un stock faible, il devrait y avoir une
            // commande
            if (level.isPurchaseDay() && level.getStockLevel() < 7) { // 7 est arbitraire, basé sur la consommation
                                                                      // hebdomadaire moyenne
                boolean foundPurchase = false;
                for (PurchaseRecommendation purchase : result.getPurchaseRecommendations()) {
                    if (purchase.getOrderDate().equals(level.getDate())) {
                        foundPurchase = true;
                        break;
                    }
                }
                assertTrue(foundPurchase, "Un achat devrait être recommandé le " + level.getDate() +
                        " car le stock est bas (" + level.getStockLevel() + ")");
            }
        }
    }
}

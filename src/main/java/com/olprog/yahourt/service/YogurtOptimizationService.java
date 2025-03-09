package com.olprog.yahourt.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.olprog.yahourt.dto.OptimizationRequestDto;
import com.olprog.yahourt.exception.ValidationException;
import com.olprog.yahourt.model.ConsumptionProfile;
import com.olprog.yahourt.model.DailyStockLevel;
import com.olprog.yahourt.model.PurchaseRecommendation;
import com.olprog.yahourt.model.SimulationResult;
import com.olprog.yahourt.model.SimulationSummary;
import com.olprog.yahourt.model.StockSimulationParams;

@Service
public class YogurtOptimizationService {

    public StockSimulationParams convertRequestToParams(OptimizationRequestDto requestDto) {
        // Commencer avec les paramètres par défaut
        StockSimulationParams params = StockSimulationParams.createDefault();

        // Mettre à jour avec les valeurs du DTO si elles sont présentes
        if (requestDto != null) {
            if (requestDto.getInitialStock() != null) {
                params.setInitialStock(requestDto.getInitialStock());
            }

            if (requestDto.getDeliveryDelay() != null) {
                params.setDeliveryDelay(requestDto.getDeliveryDelay());
            }

            if (requestDto.getPackSize() != null) {
                params.setPackSize(requestDto.getPackSize());
            }

            if (requestDto.getDailyConsumption() != null && !requestDto.getDailyConsumption().isEmpty()) {
                Map<DayOfWeek, Integer> consumptionMap = new EnumMap<>(DayOfWeek.class);

                for (Map.Entry<String, Integer> entry : requestDto.getDailyConsumption().entrySet()) {
                    try {
                        DayOfWeek dayOfWeek = DayOfWeek.valueOf(entry.getKey().toUpperCase());
                        consumptionMap.put(dayOfWeek, entry.getValue());
                    } catch (IllegalArgumentException e) {
                        throw new ValidationException("Jour de la semaine invalide: " + entry.getKey());
                    }
                }

                // Compléter avec les valeurs par défaut pour les jours non spécifiés
                for (DayOfWeek day : DayOfWeek.values()) {
                    if (!consumptionMap.containsKey(day)) {
                        consumptionMap.put(day, params.getConsumptionProfile().getConsumptionForDay(day));
                    }
                }

                ConsumptionProfile customProfile = ConsumptionProfile.builder()
                        .dailyConsumption(consumptionMap)
                        .build();
                params.setConsumptionProfile(customProfile);
            }
        }

        return params;
    }

    public SimulationResult simulateYogurtStockForYear(StockSimulationParams params) {
        // Validation des paramètres
        validateParameters(params);

        // Date de début
        LocalDate currentDate = params.getStartDate();
        // Date de fin (un an plus tard)
        LocalDate endDate = currentDate.plusYears(1);

        // Résultat
        List<PurchaseRecommendation> purchaseRecommendations = new ArrayList<>();
        List<DailyStockLevel> dailyStockLevels = new ArrayList<>();

        // Stock initial
        int currentStock = params.getInitialStock();

        // Variables pour la synthèse
        int totalYogurtsPurchased = 0;
        int totalYogurtsConsumed = 0;
        int sumStockLevels = 0;
        int minStockLevel = Integer.MAX_VALUE;
        int maxStockLevel = Integer.MIN_VALUE;

        // Simulation jour par jour
        while (currentDate.isBefore(endDate)) {
            DayOfWeek dayOfWeek = currentDate.getDayOfWeek();
            int dailyConsumption = params.getConsumptionProfile().getConsumptionForDay(dayOfWeek);

            // Vérifier si c'est un jour de livraison
            boolean isDeliveryDay = false;

            // Rechercher si une commande doit être livrée ce jour
            for (PurchaseRecommendation pr : purchaseRecommendations) {
                if (pr.getDeliveryDate().equals(currentDate)) {
                    int yogurtsDelivered = pr.getPacksToBuy() * params.getPackSize();
                    currentStock += yogurtsDelivered;
                    totalYogurtsPurchased += yogurtsDelivered;
                    isDeliveryDay = true;
                    break;
                }
            }

            // Mise à jour des statistiques
            sumStockLevels += currentStock;
            minStockLevel = Math.min(minStockLevel, currentStock);
            maxStockLevel = Math.max(maxStockLevel, currentStock);

            // Enregistrement du niveau de stock quotidien
            DailyStockLevel stockLevel = DailyStockLevel.builder()
                    .date(currentDate)
                    .stockLevel(currentStock)
                    .isDeliveryDay(isDeliveryDay)
                    .isPurchaseDay(dayOfWeek == params.getPurchaseDay())
                    .consumption(dailyConsumption)
                    .build();
            dailyStockLevels.add(stockLevel);

            // Si c'est le jour d'achat (dimanche par défaut), calculer la quantité à
            // acheter
            if (dayOfWeek == params.getPurchaseDay()) {
                // Calculer combien de yaourts seront consommés jusqu'à la prochaine livraison
                // (+ marge)
                int daysUntilNextDelivery = params.getDeliveryDelay();
                // Projection sur plus d'une semaine pour éviter les ruptures
                int daysToProject = daysUntilNextDelivery + 7;

                int projectedConsumption = 0;
                LocalDate projectedDate = currentDate;

                // Calculer la consommation projetée
                for (int i = 0; i < daysToProject; i++) {
                    DayOfWeek projectedDay = projectedDate.getDayOfWeek();
                    projectedConsumption += params.getConsumptionProfile().getConsumptionForDay(projectedDay);
                    projectedDate = projectedDate.plusDays(1);
                }

                // Calculer le nombre de paquets à acheter
                int neededYogurts = Math.max(0, projectedConsumption - currentStock);
                // Arrondir au paquet supérieur
                int packsToBuy = (int) Math.ceil((double) neededYogurts / params.getPackSize());

                if (packsToBuy > 0) {
                    PurchaseRecommendation recommendation = PurchaseRecommendation.builder()
                            .orderDate(currentDate)
                            .deliveryDate(currentDate.plusDays(params.getDeliveryDelay()))
                            .packsToBuy(packsToBuy)
                            .stockBeforePurchase(currentStock)
                            .stockAfterDelivery(currentStock + packsToBuy * params.getPackSize())
                            .build();
                    purchaseRecommendations.add(recommendation);
                }
            }

            // Consommation du jour
            int consumedToday = Math.min(currentStock, dailyConsumption);
            currentStock -= consumedToday;
            totalYogurtsConsumed += consumedToday;

            // Passer au jour suivant
            currentDate = currentDate.plusDays(1);
        }

        // Calcul des statistiques pour la synthèse
        int totalDays = dailyStockLevels.size();
        int averageStockLevel = totalDays > 0 ? sumStockLevels / totalDays : 0;
        double averagePacksToBuy = purchaseRecommendations.isEmpty() ? 0
                : purchaseRecommendations.stream().mapToInt(PurchaseRecommendation::getPacksToBuy).average().orElse(0);

        // Créer la synthèse
        SimulationSummary summary = SimulationSummary.builder()
                .totalPurchases(purchaseRecommendations.size())
                .totalYogurtsPurchased(totalYogurtsPurchased)
                .totalYogurtsConsumed(totalYogurtsConsumed)
                .averageStockLevel(averageStockLevel)
                .minimumStockLevel(minStockLevel == Integer.MAX_VALUE ? 0 : minStockLevel)
                .maximumStockLevel(maxStockLevel == Integer.MIN_VALUE ? 0 : maxStockLevel)
                .averagePacksToBuy(averagePacksToBuy)
                .build();

        // Construire et retourner le résultat
        return SimulationResult.builder()
                .purchaseRecommendations(purchaseRecommendations)
                .dailyStockLevels(dailyStockLevels)
                .summary(summary)
                .build();
    }

    private void validateParameters(StockSimulationParams params) {
        if (params == null) {
            throw new ValidationException("Les paramètres ne peuvent pas être nuls");
        }

        if (params.getStartDate() == null) {
            throw new ValidationException("La date de début ne peut pas être nulle");
        }

        if (params.getInitialStock() < 0) {
            throw new ValidationException("Le stock initial doit être supérieur ou égal à 0");
        }

        if (params.getDeliveryDelay() < 1) {
            throw new ValidationException("Le délai de livraison doit être d'au moins 1 jour");
        }

        if (params.getPackSize() < 1) {
            throw new ValidationException("La taille du paquet doit être d'au moins 1");
        }

        if (params.getPurchaseDay() == null) {
            throw new ValidationException("Le jour d'achat ne peut pas être nul");
        }

        if (params.getConsumptionProfile() == null || params.getConsumptionProfile().getDailyConsumption() == null) {
            throw new ValidationException("Le profil de consommation ne peut pas être nul");
        }

        // Vérifier que tous les jours de la semaine ont une valeur de consommation
        // valide
        for (DayOfWeek day : DayOfWeek.values()) {
            int consumption = params.getConsumptionProfile().getConsumptionForDay(day);
            if (consumption < 0) {
                throw new ValidationException("La consommation pour " + day + " doit être supérieure ou égale à 0");
            }
        }
    }
}

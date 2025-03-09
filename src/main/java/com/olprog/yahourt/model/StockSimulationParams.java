package com.olprog.yahourt.model;

import java.time.DayOfWeek;
import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockSimulationParams {
  @NotNull(message = "La date de début ne peut pas être nulle")
  private LocalDate startDate;

  @NotNull(message = "Le stock initial doit être supérieur ou égal à 0")
  private int initialStock;

  @NotNull(message = "Le délai de livraison doit être d'au moins 1 jour")
  private int deliveryDelay;

  @NotNull(message = "La taille du paquet doit être d'au moins 1")
  private int packSize;

  @NotNull(message = "Le jour d'achat ne peut pas être nul")
  private DayOfWeek purchaseDay;

  @NotNull(message = "Le profil de consommation ne peut pas être nul")
  private ConsumptionProfile consumptionProfile;

  public static StockSimulationParams createDefault() {
    return StockSimulationParams.builder()
        .startDate(LocalDate.of(2025, 1, 5))
        .initialStock(6)
        .deliveryDelay(2)
        .packSize(2)
        .purchaseDay(DayOfWeek.SUNDAY)
        .consumptionProfile(ConsumptionProfile.createDefault())
        .build();
  }
}

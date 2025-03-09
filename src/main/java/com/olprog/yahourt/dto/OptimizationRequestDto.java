package com.olprog.yahourt.dto;

import java.util.Map;

import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OptimizationRequestDto {
  @Min(value = 0, message = "Le stock initial doit être supérieur ou égal à 0")
  private Integer initialStock;

  @Min(value = 1, message = "Le délai de livraison doit être d'au moins 1 jour")
  private Integer deliveryDelay;

  @Min(value = 1, message = "La taille du paquet doit être d'au moins 1")
  private Integer packSize;

  private Map<String, Integer> dailyConsumption;
}

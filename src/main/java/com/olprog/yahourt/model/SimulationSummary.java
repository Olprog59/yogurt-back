package com.olprog.yahourt.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SimulationSummary {
  private int totalPurchases;
  private int totalYogurtsPurchased;
  private int totalYogurtsConsumed;
  private int averageStockLevel;
  private int minimumStockLevel;
  private int maximumStockLevel;
  private double averagePacksToBuy;
}

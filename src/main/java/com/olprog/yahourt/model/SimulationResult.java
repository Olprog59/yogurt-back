package com.olprog.yahourt.model;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SimulationResult {

  private List<PurchaseRecommendation> purchaseRecommendations;
  private List<DailyStockLevel> dailyStockLevels;
  private SimulationSummary summary;
}

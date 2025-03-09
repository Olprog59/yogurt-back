package com.olprog.yahourt.model;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PurchaseRecommendation {
  private LocalDate orderDate;
  private LocalDate deliveryDate;
  private int packsToBuy;
  private int stockBeforePurchase;
  private int stockAfterDelivery;
}

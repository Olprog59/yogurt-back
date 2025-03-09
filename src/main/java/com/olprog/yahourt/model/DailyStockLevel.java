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
public class DailyStockLevel {
  private LocalDate date;
  private int stockLevel;
  private boolean isDeliveryDay;
  private boolean isPurchaseDay;
  private int consumption;
}

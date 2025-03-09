package com.olprog.yahourt.model;

import java.time.DayOfWeek;
import java.util.EnumMap;
import java.util.Map;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConsumptionProfile {

  @NotNull(message = "La consommation quotidienne ne peut pas être nulle")
  private Map<DayOfWeek, Integer> dailyConsumption;

  public static ConsumptionProfile createDefault() {
    Map<DayOfWeek, Integer> defaultConsumption = new EnumMap<>(DayOfWeek.class);
    defaultConsumption.put(DayOfWeek.MONDAY, 3);
    defaultConsumption.put(DayOfWeek.TUESDAY, 3);
    defaultConsumption.put(DayOfWeek.WEDNESDAY, 3);
    defaultConsumption.put(DayOfWeek.THURSDAY, 3);
    defaultConsumption.put(DayOfWeek.FRIDAY, 3);
    defaultConsumption.put(DayOfWeek.SATURDAY, 4);
    defaultConsumption.put(DayOfWeek.SUNDAY, 4);
    return ConsumptionProfile.builder()
        .dailyConsumption(defaultConsumption).build();
  }

  public int getConsumptionForDay(DayOfWeek dayOfWeek) {
    return dailyConsumption.getOrDefault(dayOfWeek, 0);
  }

}

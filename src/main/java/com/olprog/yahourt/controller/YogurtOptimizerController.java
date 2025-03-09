package com.olprog.yahourt.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.olprog.yahourt.dto.OptimizationRequestDto;
import com.olprog.yahourt.model.SimulationResult;
import com.olprog.yahourt.model.StockSimulationParams;
import com.olprog.yahourt.service.YogurtOptimizationService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/yogurt")
@CrossOrigin(origins = "*")
@Validated
public class YogurtOptimizerController {

    private final YogurtOptimizationService optimizationService;

    @Autowired
    public YogurtOptimizerController(YogurtOptimizationService optimizationService) {
        this.optimizationService = optimizationService;
    }

    @PostMapping("/optimize")
    public ResponseEntity<SimulationResult> optimizeYogurtStock(
            @RequestBody(required = false) @Valid OptimizationRequestDto requestDto) {
        // Convertir le DTO en paramètres de simulation
        StockSimulationParams params = optimizationService.convertRequestToParams(requestDto);

        // Exécuter la simulation
        SimulationResult result = optimizationService.simulateYogurtStockForYear(params);

        return ResponseEntity.ok(result);
    }

}

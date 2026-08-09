package com.eternalprotocol.api.controller;

import com.eternalprotocol.api.dto.*;
import com.eternalprotocol.api.service.AthleteService;
import com.eternalprotocol.api.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Everything here requires the ADMIN role — enforced centrally in
 * SecurityConfig for the whole /api/admin/** path, rather than per-method,
 * so there's a single source of truth for "what needs admin access."
 */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final OrderService orderService;
    private final AthleteService athleteService;

    public AdminController(OrderService orderService, AthleteService athleteService) {
        this.orderService = orderService;
        this.athleteService = athleteService;
    }

    // ---- Orders ----

    @GetMapping("/orders")
    public ResponseEntity<List<AdminOrderDto>> getAllOrders(@RequestParam(required = false) String status) {
        return ResponseEntity.ok(orderService.getAllOrdersForAdmin(status));
    }

    // ---- Athletes ----

    @GetMapping("/athletes")
    public ResponseEntity<List<AthleteDto>> getAllAthletes() {
        return ResponseEntity.ok(athleteService.getAllAthletes());
    }

    @PostMapping("/athletes")
    public ResponseEntity<AthleteDto> createAthlete(@Valid @RequestBody CreateAthleteRequestDto request) {
        return ResponseEntity.ok(athleteService.createAthlete(request));
    }

    @PutMapping("/athletes/{id}")
    public ResponseEntity<AthleteDto> updateAthlete(@PathVariable Long id,
                                                      @Valid @RequestBody UpdateAthleteRequestDto request) {
        return ResponseEntity.ok(athleteService.updateAthlete(id, request));
    }

    // ---- Commissions ----

    @GetMapping("/commissions")
    public ResponseEntity<List<CommissionDto>> getAllCommissions(@RequestParam(required = false) String status) {
        return ResponseEntity.ok(athleteService.getAllCommissions(status));
    }

    @PutMapping("/commissions/{id}/mark-paid")
    public ResponseEntity<CommissionDto> markCommissionPaid(@PathVariable Long id) {
        return ResponseEntity.ok(athleteService.markCommissionPaid(id));
    }
}

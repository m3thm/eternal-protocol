package com.eternalprotocol.api.controller;

import com.eternalprotocol.api.dto.AthleteCodeValidationDto;
import com.eternalprotocol.api.dto.AthleteDashboardDto;
import com.eternalprotocol.api.security.CurrentUser;
import com.eternalprotocol.api.service.AthleteService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/athletes")
public class AthleteController {

    private final AthleteService athleteService;
    private final CurrentUser currentUser;

    public AthleteController(AthleteService athleteService, CurrentUser currentUser) {
        this.athleteService = athleteService;
        this.currentUser = currentUser;
    }

    // Public — used by the checkout page's live discount preview.
    @GetMapping("/validate-code/{code}")
    public ResponseEntity<AthleteCodeValidationDto> validateCode(@PathVariable String code) {
        return ResponseEntity.ok(athleteService.validateCode(code));
    }

    // Authenticated athlete only. Athlete ID always comes from the JWT via
    // CurrentUser, never from a path variable — an athlete can't view
    // another athlete's data by guessing an ID.
    @GetMapping("/dashboard")
    public ResponseEntity<AthleteDashboardDto> getDashboard() {
        return ResponseEntity.ok(athleteService.getDashboard(currentUser.id()));
    }
}

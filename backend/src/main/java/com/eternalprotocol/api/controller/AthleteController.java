package com.eternalprotocol.api.controller;

import com.eternalprotocol.api.dto.AthleteCodeValidationDto;
import com.eternalprotocol.api.dto.AthleteDashboardDto;
import com.eternalprotocol.api.security.CurrentUser;
import com.eternalprotocol.api.service.AthleteService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST endpoints for validating discount codes and an athlete's own
 * dashboard.
 * <p>
 * Athlete account management (create/update/deactivate by an admin) lives
 * in {@link AdminController} instead, since those actions are performed by
 * an admin, not by the athlete themself.
 */
@RestController
@RequestMapping("/api/athletes")
public class AthleteController {

    private final AthleteService athleteService;
    private final CurrentUser currentUser;

    /**
     * @param athleteService code validation and dashboard logic
     * @param currentUser    resolves the logged-in athlete's ID from the JWT
     */
    public AthleteController(AthleteService athleteService, CurrentUser currentUser) {
        this.athleteService = athleteService;
        this.currentUser = currentUser;
    }

    /**
     * Public. Checks whether a discount code exists and is active, and
     * returns its discount percentage. Used by the checkout page to show a
     * live discount preview as the customer types a code.
     *
     * @param code the code to check
     * @return whether the code is valid, and its discount percentage
     */
    @GetMapping("/validate-code/{code}")
    public ResponseEntity<AthleteCodeValidationDto> validateCode(@PathVariable String code) {
        return ResponseEntity.ok(athleteService.validateCode(code));
    }

    /**
     * Athlete-only. Gets the logged-in athlete's own dashboard: their
     * order and commission totals. The athlete's ID always comes from the
     * JWT via {@link CurrentUser}, never from a path variable, so an
     * athlete can't view another athlete's dashboard by guessing an ID.
     *
     * @return the logged-in athlete's dashboard data
     */
    @GetMapping("/dashboard")
    public ResponseEntity<AthleteDashboardDto> getDashboard() {
        return ResponseEntity.ok(athleteService.getDashboard(currentUser.id()));
    }
}

package com.eternalprotocol.api.service;

import com.eternalprotocol.api.dto.*;
import com.eternalprotocol.api.entity.Athlete;
import com.eternalprotocol.api.entity.Commission;
import com.eternalprotocol.api.entity.CommissionStatus;
import com.eternalprotocol.api.exception.EmailAlreadyInUseException;
import com.eternalprotocol.api.exception.ResourceNotFoundException;
import com.eternalprotocol.api.repository.AthleteRepository;
import com.eternalprotocol.api.repository.CommissionRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class AthleteService {

    private final AthleteRepository athleteRepository;
    private final CommissionRepository commissionRepository;
    private final PasswordEncoder passwordEncoder;

    public AthleteService(AthleteRepository athleteRepository,
                           CommissionRepository commissionRepository,
                           PasswordEncoder passwordEncoder) {
        this.athleteRepository = athleteRepository;
        this.commissionRepository = commissionRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /** Public endpoint — used for the live discount preview on the checkout page. */
    public AthleteCodeValidationDto validateCode(String code) {
        return athleteRepository.findByAthleteCodeIgnoreCase(code)
                .filter(Athlete::isActive)
                .map(a -> new AthleteCodeValidationDto(true, a.getAthleteCode(), a.getCommissionRate()))
                .orElseGet(() -> AthleteCodeValidationDto.invalid(code));
    }

    /**
     * Builds the athlete's own dashboard. `athleteId` must come from the
     * JWT (SecurityContextHolder), never a client-supplied path variable —
     * see AthleteController — so one athlete can never view another's data.
     */
    @Transactional(readOnly = true)
    public AthleteDashboardDto getDashboard(Long athleteId) {
        Athlete athlete = athleteRepository.findById(athleteId)
                .orElseThrow(() -> new ResourceNotFoundException("Athlete not found"));

        List<Commission> commissions = commissionRepository.findByAthleteId(athleteId);

        BigDecimal salesGenerated = commissions.stream()
                .map(c -> c.getOrder().getSubtotalAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal earned = sumByStatus(commissions, CommissionStatus.EARNED)
                .add(sumByStatus(commissions, CommissionStatus.PAID));
        BigDecimal paid = sumByStatus(commissions, CommissionStatus.PAID);
        BigDecimal pending = sumByStatus(commissions, CommissionStatus.EARNED);

        List<AthleteOrderSummaryDto> orderSummaries = commissions.stream()
                .map(c -> new AthleteOrderSummaryDto(
                        c.getOrder().getId(),
                        OrderService.toLineItemDtos(c.getOrder()),
                        c.getOrder().getOrderDate(),
                        c.getOrder().getPaymentStatus().name(),
                        c.getCommissionAmount(),
                        c.getCommissionStatus().name()))
                .toList();

        return new AthleteDashboardDto(
                athlete.getAthleteCode(), athlete.getCommissionRate(),
                commissions.size(), salesGenerated, earned, paid, pending, orderSummaries);
    }

    private BigDecimal sumByStatus(List<Commission> commissions, CommissionStatus status) {
        return commissions.stream()
                .filter(c -> c.getCommissionStatus() == status)
                .map(Commission::getCommissionAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // ---- Admin: athlete management ----

    public List<AthleteDto> getAllAthletes() {
        return athleteRepository.findAll().stream().map(this::toDto).toList();
    }

    public AthleteDto createAthlete(CreateAthleteRequestDto request) {
        if (athleteRepository.existsByEmail(request.email())) {
            throw new EmailAlreadyInUseException("An account with this email already exists");
        }
        if (athleteRepository.existsByAthleteCodeIgnoreCase(request.athleteCode())) {
            throw new IllegalArgumentException("Athlete code already in use: " + request.athleteCode());
        }

        Athlete athlete = new Athlete();
        athlete.setName(request.name());
        athlete.setEmail(request.email());
        athlete.setPasswordHash(passwordEncoder.encode(request.password()));
        athlete.setAthleteCode(request.athleteCode());
        athlete.setCommissionRate(request.commissionRate());
        athlete.setActive(true);

        return toDto(athleteRepository.save(athlete));
    }

    public AthleteDto updateAthlete(Long id, UpdateAthleteRequestDto request) {
        Athlete athlete = athleteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Athlete not found: " + id));

        if (request.commissionRate() != null) {
            athlete.setCommissionRate(request.commissionRate());
        }
        if (request.active() != null) {
            athlete.setActive(request.active());
        }

        return toDto(athleteRepository.save(athlete));
    }

    // ---- Admin: commission management ----

    @Transactional(readOnly = true)
    public List<CommissionDto> getAllCommissions(String statusFilter) {
        List<Commission> commissions;
        if (statusFilter != null && !statusFilter.isBlank()) {
            CommissionStatus status = CommissionStatus.valueOf(statusFilter.toUpperCase());
            commissions = commissionRepository.findByCommissionStatus(status);
        } else {
            commissions = commissionRepository.findAll();
        }
        return commissions.stream().map(this::toCommissionDto).toList();
    }

    @Transactional
    public CommissionDto markCommissionPaid(Long commissionId) {
        Commission commission = commissionRepository.findById(commissionId)
                .orElseThrow(() -> new ResourceNotFoundException("Commission not found: " + commissionId));

        if (commission.getCommissionStatus() != CommissionStatus.EARNED) {
            throw new IllegalArgumentException(
                    "Only commissions with status EARNED can be marked as paid (current: "
                            + commission.getCommissionStatus() + ")");
        }

        commission.setCommissionStatus(CommissionStatus.PAID);
        commission.setPaidDate(LocalDateTime.now());
        return toCommissionDto(commissionRepository.save(commission));
    }

    private AthleteDto toDto(Athlete a) {
        return new AthleteDto(a.getId(), a.getName(), a.getEmail(), a.getAthleteCode(),
                a.getCommissionRate(), a.isActive());
    }

    private CommissionDto toCommissionDto(Commission c) {
        return new CommissionDto(c.getId(), c.getAthlete().getName(), c.getAthlete().getAthleteCode(),
                c.getOrder().getId(), c.getCommissionAmount(), c.getCommissionStatus().name(), c.getPaidDate());
    }
}

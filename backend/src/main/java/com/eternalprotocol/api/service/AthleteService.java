package com.eternalprotocol.api.service;

import com.eternalprotocol.api.cache.AthleteCodeCache;
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

/**
 * Business logic for discount code validation, an athlete's own dashboard,
 * and admin management of athletes and their commissions.
 */
@Service
public class AthleteService {

    private final AthleteRepository athleteRepository;
    private final CommissionRepository commissionRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * @param athleteRepository    access to athlete data
     * @param commissionRepository access to commission data
     * @param passwordEncoder      hashes new athlete passwords
     */
    public AthleteService(AthleteRepository athleteRepository,
                           CommissionRepository commissionRepository,
                           PasswordEncoder passwordEncoder) {
        this.athleteRepository = athleteRepository;
        this.commissionRepository = commissionRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Checks whether a discount code exists and is active. Used by the
     * checkout page's live discount preview as a customer types a code —
     * a fast-repeating call for the same one or two codes, which is why
     * this method checks {@link AthleteCodeCache} before ever touching the
     * database, and populates it on a miss.
     *
     * @param code the code to check
     * @return whether the code is valid, and its discount percentage
     */
    public AthleteCodeValidationDto validateCode(String code) {
        AthleteCodeCache cache = AthleteCodeCache.getInstance();

        return cache.get(code).orElseGet(() -> {
            AthleteCodeValidationDto result = athleteRepository.findByAthleteCodeIgnoreCase(code)
                    .filter(Athlete::isActive)
                    .map(a -> new AthleteCodeValidationDto(true, a.getAthleteCode(), a.getCommissionRate()))
                    .orElseGet(() -> AthleteCodeValidationDto.invalid(code));
            cache.put(code, result);
            return result;
        });
    }

    /**
     * Builds an athlete's own dashboard: their commission totals and
     * order history. {@code athleteId} must always come from the verified
     * JWT (see {@code CurrentUser} / {@code AthleteController}), never
     * from a client-supplied path variable — otherwise one athlete could
     * view another athlete's dashboard just by guessing an ID.
     *
     * @param athleteId athlete's database ID, taken from the JWT
     * @return the athlete's dashboard totals and order list
     * @throws ResourceNotFoundException if no athlete exists with this ID
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

    /**
     * Sums the commission amount of every commission in a given status.
     *
     * @param commissions the commissions to sum
     * @param status      status to filter by before summing
     * @return the total amount for commissions in that status
     */
    private BigDecimal sumByStatus(List<Commission> commissions, CommissionStatus status) {
        return commissions.stream()
                .filter(c -> c.getCommissionStatus() == status)
                .map(Commission::getCommissionAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Gets every athlete account, for the admin dashboard.
     *
     * @return all athletes
     */
    public List<AthleteDto> getAllAthletes() {
        return athleteRepository.findAll().stream().map(this::toDto).toList();
    }

    /**
     * Creates a new athlete account.
     *
     * @param request new athlete's details
     * @return the created athlete
     * @throws EmailAlreadyInUseException if the email is already registered
     * @throws IllegalArgumentException   if the athlete code is already taken
     */
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

    /**
     * Updates an existing athlete's commission rate and/or active status.
     * Any field left null in the request is left unchanged. Evicts this
     * athlete's code from {@link AthleteCodeCache} afterward, since either
     * change affects what {@link #validateCode} should return for it —
     * without this, a customer could keep seeing a stale cached discount
     * rate, or a deactivated code as still valid, for up to the cache's
     * TTL after an admin changed it.
     *
     * @param id      athlete's database ID
     * @param request fields to change
     * @return the updated athlete
     * @throws ResourceNotFoundException if no athlete exists with this ID
     */
    public AthleteDto updateAthlete(Long id, UpdateAthleteRequestDto request) {
        Athlete athlete = athleteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Athlete not found: " + id));

        if (request.commissionRate() != null) {
            athlete.setCommissionRate(request.commissionRate());
        }
        if (request.active() != null) {
            athlete.setActive(request.active());
        }

        AthleteDto updated = toDto(athleteRepository.save(athlete));
        AthleteCodeCache.getInstance().evict(athlete.getAthleteCode());
        return updated;
    }

    /**
     * Gets every commission in the system, optionally filtered by status.
     *
     * @param statusFilter status name to filter by (e.g. "EARNED"), or null/blank for all
     * @return matching commissions
     */
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

    /**
     * Marks a commission as paid out to its athlete.
     *
     * @param commissionId commission's database ID
     * @return the updated commission
     * @throws ResourceNotFoundException if no commission exists with this ID
     * @throws IllegalArgumentException  if the commission isn't currently in EARNED status
     */
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

    /**
     * Converts a database entity into its public-facing DTO shape.
     *
     * @param a the entity to convert
     * @return the equivalent DTO
     */
    private AthleteDto toDto(Athlete a) {
        return new AthleteDto(a.getId(), a.getName(), a.getEmail(), a.getAthleteCode(),
                a.getCommissionRate(), a.isActive());
    }

    /**
     * Converts a database entity into its public-facing DTO shape.
     *
     * @param c the entity to convert
     * @return the equivalent DTO
     */
    private CommissionDto toCommissionDto(Commission c) {
        return new CommissionDto(c.getId(), c.getAthlete().getName(), c.getAthlete().getAthleteCode(),
                c.getOrder().getId(), c.getCommissionAmount(), c.getCommissionStatus().name(), c.getPaidDate());
    }
}

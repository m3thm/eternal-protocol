package com.eternalprotocol.api.service;

import com.eternalprotocol.api.cache.AthleteCodeCache;
import com.eternalprotocol.api.dto.*;
import com.eternalprotocol.api.entity.*;
import com.eternalprotocol.api.exception.EmailAlreadyInUseException;
import com.eternalprotocol.api.exception.ResourceNotFoundException;
import com.eternalprotocol.api.repository.AthleteRepository;
import com.eternalprotocol.api.repository.CommissionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AthleteServiceTest {

    @Mock
    private AthleteRepository athleteRepository;
    @Mock
    private CommissionRepository commissionRepository;
    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AthleteService athleteService;

    private Athlete athlete;
    private Product product;
    private Customer customer;

    @BeforeEach
    void setUp() {
        // AthleteService.validateCode reads through AthleteCodeCache, which
        // is a genuine JVM-wide singleton (see that class) — its state
        // persists across test methods and even across test classes run in
        // the same JVM. Without clearing it here, a code cached as "valid"
        // by one test (e.g. "RISE10") could leak into a later test that
        // expects the same code to now be invalid, causing that test to see
        // a stale cached result instead of exercising the repository mock
        // it just set up. This is a genuine, well-known trade-off of the
        // Singleton pattern — global mutable state is convenient in
        // production and awkward in tests — worth knowing for the viva.
        AthleteCodeCache.getInstance().clear();

        athlete = new Athlete();
        athlete.setId(7L);
        athlete.setName("Usain Bolt");
        athlete.setEmail("usain@example.com");
        athlete.setAthleteCode("RISE10");
        athlete.setCommissionRate(new BigDecimal("10.00"));
        athlete.setActive(true);

        product = new Product();
        product.setId(1L);
        product.setName("Protocol 001 Oversized Tee");
        product.setPrice(new BigDecimal("4500.00"));

        customer = new Customer("Jane Doe", "0771234567", "123 Main St");
    }

    // ---- validateCode ----

    @Test
    void validateCode_activeAthlete_returnsValidWithDiscountRate() {
        when(athleteRepository.findByAthleteCodeIgnoreCase("RISE10")).thenReturn(Optional.of(athlete));

        AthleteCodeValidationDto result = athleteService.validateCode("RISE10");

        assertThat(result.valid()).isTrue();
        assertThat(result.discountPercentage()).isEqualByComparingTo("10.00");
    }

    @Test
    void validateCode_inactiveAthlete_returnsInvalid() {
        athlete.setActive(false);
        when(athleteRepository.findByAthleteCodeIgnoreCase("RISE10")).thenReturn(Optional.of(athlete));

        AthleteCodeValidationDto result = athleteService.validateCode("RISE10");

        assertThat(result.valid()).isFalse();
    }

    @Test
    void validateCode_unknownCode_returnsInvalid() {
        when(athleteRepository.findByAthleteCodeIgnoreCase("NOPE")).thenReturn(Optional.empty());

        AthleteCodeValidationDto result = athleteService.validateCode("NOPE");

        assertThat(result.valid()).isFalse();
        assertThat(result.code()).isEqualTo("NOPE");
    }

    // ---- getDashboard ----

    private Order buildOrderWithOneItem(Long orderId, BigDecimal discount, String size, String colour) {
        Order order = new Order(customer, new BigDecimal("4500.00"), discount, "RISE10");
        order.setId(orderId);
        order.addItem(new OrderItem(order, product, size, colour, 1, new BigDecimal("4500.00")));
        return order;
    }

    @Test
    void getDashboard_computesTotalsAcrossCommissionStatuses() {
        Order order1 = buildOrderWithOneItem(101L, new BigDecimal("450.00"), "M", "Black");
        Commission earned = new Commission(athlete, order1, new BigDecimal("450.00"));
        earned.setCommissionStatus(CommissionStatus.EARNED);

        Order order2 = buildOrderWithOneItem(102L, new BigDecimal("450.00"), "L", "White");
        Commission paid = new Commission(athlete, order2, new BigDecimal("450.00"));
        paid.setCommissionStatus(CommissionStatus.PAID);

        Order order3 = buildOrderWithOneItem(103L, new BigDecimal("450.00"), "S", "Black");
        Commission awaitingPayment = new Commission(athlete, order3, new BigDecimal("450.00"));
        awaitingPayment.setCommissionStatus(CommissionStatus.AWAITING_PAYMENT);

        when(athleteRepository.findById(7L)).thenReturn(Optional.of(athlete));
        when(commissionRepository.findByAthleteId(7L)).thenReturn(List.of(earned, paid, awaitingPayment));

        AthleteDashboardDto dashboard = athleteService.getDashboard(7L);

        assertThat(dashboard.totalOrders()).isEqualTo(3);
        assertThat(dashboard.salesGenerated()).isEqualByComparingTo("13500.00"); // 3 x 4500 subtotal
        assertThat(dashboard.commissionEarned()).isEqualByComparingTo("900.00"); // EARNED + PAID
        assertThat(dashboard.paidCommission()).isEqualByComparingTo("450.00"); // PAID only
        assertThat(dashboard.pendingCommission()).isEqualByComparingTo("450.00"); // EARNED only
        assertThat(dashboard.orders()).hasSize(3);
        // Each order summary now carries its own line items, not a single productName.
        assertThat(dashboard.orders().get(0).items()).hasSize(1);
        assertThat(dashboard.orders().get(0).items().get(0).productName()).isEqualTo("Protocol 001 Oversized Tee");
    }

    @Test
    void getDashboard_orderWithMultipleItems_dashboardShowsAllOfThem() {
        Product hoodie = new Product();
        hoodie.setId(2L);
        hoodie.setName("Protocol 002 Hoodie");
        hoodie.setPrice(new BigDecimal("8000.00"));

        Order order = new Order(customer, new BigDecimal("17000.00"), new BigDecimal("1700.00"), "RISE10");
        order.setId(101L);
        order.addItem(new OrderItem(order, product, "M", "Black", 2, new BigDecimal("4500.00")));
        order.addItem(new OrderItem(order, hoodie, "L", "Grey", 1, new BigDecimal("8000.00")));
        Commission commission = new Commission(athlete, order, new BigDecimal("1700.00"));
        commission.setCommissionStatus(CommissionStatus.EARNED);

        when(athleteRepository.findById(7L)).thenReturn(Optional.of(athlete));
        when(commissionRepository.findByAthleteId(7L)).thenReturn(List.of(commission));

        AthleteDashboardDto dashboard = athleteService.getDashboard(7L);

        assertThat(dashboard.orders()).hasSize(1);
        assertThat(dashboard.orders().get(0).items()).hasSize(2);
        assertThat(dashboard.salesGenerated()).isEqualByComparingTo("17000.00");
    }

    @Test
    void getDashboard_unknownAthlete_throwsResourceNotFoundException() {
        when(athleteRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> athleteService.getDashboard(404L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ---- admin: createAthlete ----

    @Test
    void createAthlete_validRequest_encodesPasswordAndSaves() {
        CreateAthleteRequestDto request = new CreateAthleteRequestDto(
                "New Athlete", "new@example.com", "password123", "NEWCODE", new BigDecimal("15.00"));

        when(athleteRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(athleteRepository.existsByAthleteCodeIgnoreCase("NEWCODE")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashed");
        when(athleteRepository.save(any(Athlete.class))).thenAnswer(inv -> {
            Athlete a = inv.getArgument(0);
            a.setId(99L);
            return a;
        });

        AthleteDto result = athleteService.createAthlete(request);

        assertThat(result.id()).isEqualTo(99L);
        assertThat(result.athleteCode()).isEqualTo("NEWCODE");
        assertThat(result.active()).isTrue();
    }

    @Test
    void createAthlete_duplicateEmail_throwsEmailAlreadyInUse() {
        CreateAthleteRequestDto request = new CreateAthleteRequestDto(
                "New Athlete", "existing@example.com", "password123", "NEWCODE", new BigDecimal("15.00"));
        when(athleteRepository.existsByEmail("existing@example.com")).thenReturn(true);

        assertThatThrownBy(() -> athleteService.createAthlete(request))
                .isInstanceOf(EmailAlreadyInUseException.class);
        verify(athleteRepository, never()).save(any());
    }

    @Test
    void createAthlete_duplicateCode_throwsIllegalArgument() {
        CreateAthleteRequestDto request = new CreateAthleteRequestDto(
                "New Athlete", "new@example.com", "password123", "RISE10", new BigDecimal("15.00"));
        when(athleteRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(athleteRepository.existsByAthleteCodeIgnoreCase("RISE10")).thenReturn(true);

        assertThatThrownBy(() -> athleteService.createAthlete(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("RISE10");
        verify(athleteRepository, never()).save(any());
    }

    // ---- admin: updateAthlete ----

    @Test
    void updateAthlete_onlyCommissionRateProvided_leavesActiveUnchanged() {
        UpdateAthleteRequestDto request = new UpdateAthleteRequestDto(new BigDecimal("20.00"), null);
        when(athleteRepository.findById(7L)).thenReturn(Optional.of(athlete));
        when(athleteRepository.save(any(Athlete.class))).thenAnswer(inv -> inv.getArgument(0));

        AthleteDto result = athleteService.updateAthlete(7L, request);

        assertThat(result.commissionRate()).isEqualByComparingTo("20.00");
        assertThat(result.active()).isTrue(); // unchanged from setUp()
    }

    @Test
    void updateAthlete_onlyActiveProvided_leavesCommissionRateUnchanged() {
        UpdateAthleteRequestDto request = new UpdateAthleteRequestDto(null, false);
        when(athleteRepository.findById(7L)).thenReturn(Optional.of(athlete));
        when(athleteRepository.save(any(Athlete.class))).thenAnswer(inv -> inv.getArgument(0));

        AthleteDto result = athleteService.updateAthlete(7L, request);

        assertThat(result.active()).isFalse();
        assertThat(result.commissionRate()).isEqualByComparingTo("10.00"); // unchanged from setUp()
    }

    @Test
    void updateAthlete_unknownId_throwsResourceNotFoundException() {
        UpdateAthleteRequestDto request = new UpdateAthleteRequestDto(new BigDecimal("20.00"), null);
        when(athleteRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> athleteService.updateAthlete(404L, request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ---- admin: commission management ----

    @Test
    void markCommissionPaid_earnedCommission_flipsToPaidAndSetsDate() {
        Order order = new Order(customer, new BigDecimal("4500.00"), new BigDecimal("450.00"), "RISE10");
        order.setId(101L);
        Commission commission = new Commission(athlete, order, new BigDecimal("450.00"));
        commission.setId(50L);
        commission.setCommissionStatus(CommissionStatus.EARNED);

        when(commissionRepository.findById(50L)).thenReturn(Optional.of(commission));
        when(commissionRepository.save(any(Commission.class))).thenAnswer(inv -> inv.getArgument(0));

        CommissionDto result = athleteService.markCommissionPaid(50L);

        assertThat(result.commissionStatus()).isEqualTo("PAID");
        assertThat(result.paidDate()).isNotNull();
    }

    @Test
    void markCommissionPaid_notYetEarned_throwsIllegalArgument() {
        Order order = new Order(customer, new BigDecimal("4500.00"), new BigDecimal("450.00"), "RISE10");
        order.setId(101L);
        Commission commission = new Commission(athlete, order, new BigDecimal("450.00"));
        commission.setId(50L);
        commission.setCommissionStatus(CommissionStatus.AWAITING_PAYMENT); // payment not confirmed yet

        when(commissionRepository.findById(50L)).thenReturn(Optional.of(commission));

        assertThatThrownBy(() -> athleteService.markCommissionPaid(50L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("EARNED");
        verify(commissionRepository, never()).save(any());
    }

    @Test
    void markCommissionPaid_alreadyPaid_throwsIllegalArgument() {
        Order order = new Order(customer, new BigDecimal("4500.00"), new BigDecimal("450.00"), "RISE10");
        order.setId(101L);
        Commission commission = new Commission(athlete, order, new BigDecimal("450.00"));
        commission.setId(50L);
        commission.setCommissionStatus(CommissionStatus.PAID); // already paid once

        when(commissionRepository.findById(50L)).thenReturn(Optional.of(commission));

        assertThatThrownBy(() -> athleteService.markCommissionPaid(50L))
                .isInstanceOf(IllegalArgumentException.class);
    }
}

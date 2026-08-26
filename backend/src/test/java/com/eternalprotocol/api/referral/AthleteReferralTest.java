package com.eternalprotocol.api.referral;

import com.eternalprotocol.api.entity.Athlete;
import com.eternalprotocol.api.entity.Order;
import com.eternalprotocol.api.repository.CommissionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AthleteReferralTest {

    @Mock
    private CommissionRepository commissionRepository;
    @Mock
    private Order order;

    private Athlete athlete(String code, String rate) {
        return new Athlete("Jane Athlete", "jane@example.com", "hash", code, new BigDecimal(rate), true);
    }

    // --- NoAthleteReferral (the Null Object) ---

    @Test
    void noAthleteReferral_calculateDiscount_isAlwaysZero() {
        BigDecimal discount = NoAthleteReferral.INSTANCE.calculateDiscount(new BigDecimal("10000"));

        assertThat(discount).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void noAthleteReferral_athleteCodeForOrder_isNull() {
        assertThat(NoAthleteReferral.INSTANCE.athleteCodeForOrder()).isNull();
    }

    @Test
    void noAthleteReferral_recordCommission_doesNothing() {
        NoAthleteReferral.INSTANCE.recordCommission(commissionRepository, order, new BigDecimal("500"));

        verifyNoInteractions(commissionRepository);
    }

    @Test
    void noAthleteReferral_instance_isASingleSharedInstance() {
        assertThat(NoAthleteReferral.INSTANCE).isSameAs(NoAthleteReferral.INSTANCE);
    }

    // --- AppliedAthleteReferral ---

    @Test
    void appliedAthleteReferral_calculateDiscount_appliesTheAthletesCommissionRate() {
        AthleteReferral referral = new AppliedAthleteReferral(athlete("RISE45", "5.00"));

        BigDecimal discount = referral.calculateDiscount(new BigDecimal("10000"));

        assertThat(discount).isEqualByComparingTo(new BigDecimal("500.00"));
    }

    @Test
    void appliedAthleteReferral_athleteCodeForOrder_returnsTheAthletesCode() {
        AthleteReferral referral = new AppliedAthleteReferral(athlete("RISE45", "5.00"));

        assertThat(referral.athleteCodeForOrder()).isEqualTo("RISE45");
    }

    @Test
    void appliedAthleteReferral_recordCommission_savesACommission() {
        AthleteReferral referral = new AppliedAthleteReferral(athlete("RISE45", "5.00"));

        referral.recordCommission(commissionRepository, order, new BigDecimal("500"));

        verify(commissionRepository).save(any());
    }
}

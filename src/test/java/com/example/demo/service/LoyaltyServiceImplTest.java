package com.example.demo.service;

import com.example.demo.dto.LoyaltySummaryDTO;
import com.example.demo.dto.PointAdjustmentRequestDTO;
import com.example.demo.dto.PointTransactionDTO;
import com.example.demo.dto.RedeemedRewardDTO;
import com.example.demo.entities.*;
import com.example.demo.repository.ClientRepository;
import com.example.demo.repository.PointTransactionRepository;
import com.example.demo.repository.RedeemedRewardRepository;
import com.example.demo.repository.RewardRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoyaltyServiceImplTest {

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private RewardRepository rewardRepository;

    @Mock
    private RedeemedRewardRepository redeemedRewardRepository;

    @Mock
    private PointTransactionRepository pointTransactionRepository;

    private LoyaltyServiceImpl loyaltyService;

    @BeforeEach
    void setUp() {
        loyaltyService = new LoyaltyServiceImpl(
                clientRepository,
                rewardRepository,
                redeemedRewardRepository,
                pointTransactionRepository);
    }

    @Test
    void redeemReward_shouldSubtractPointsDecrementStockCreateRedemptionAndRecordTransaction() {
        Client client = Client.builder()
                .id(1L)
                .firstName("Samuel")
                .lastName("Nemes")
                .points(3000)
                .lifetimePoints(3000)
                .loyaltyTier(LoyaltyTier.PRIVILEGE)
                .build();

        Reward reward = Reward.builder()
                .id(10L)
                .name("Masaje Relajante Spa")
                .category("SPA")
                .pointsCost(2000)
                .minimumTier(LoyaltyTier.CLASSIC)
                .stock(5)
                .active(true)
                .build();

        when(clientRepository.findById(1L)).thenReturn(Optional.of(client));
        when(rewardRepository.findById(10L)).thenReturn(Optional.of(reward));
        when(redeemedRewardRepository.findByRedemptionCodeIgnoreCase(anyString())).thenReturn(Optional.empty());
        when(redeemedRewardRepository.save(any(RedeemedReward.class))).thenAnswer(inv -> {
            RedeemedReward r = inv.getArgument(0);
            r.setId(99L);
            return r;
        });
        when(pointTransactionRepository.save(any(PointTransaction.class))).thenAnswer(inv -> inv.getArgument(0));

        RedeemedRewardDTO result = loyaltyService.redeemReward(1L, 10L);

        assertThat(result.getId()).isEqualTo(99L);
        assertThat(result.getPointsSpent()).isEqualTo(2000);
        assertThat(result.getStatus()).isEqualTo("ACTIVE");
        assertThat(result.getRedemptionCode()).startsWith("MC-RWD-");

        // Saldo descontado, lifetimePoints y categoría conservados
        assertThat(client.getPoints()).isEqualTo(1000);
        assertThat(client.getLifetimePoints()).isEqualTo(3000);
        assertThat(client.getLoyaltyTier()).isEqualTo(LoyaltyTier.PRIVILEGE);
        assertThat(reward.getStock()).isEqualTo(4);

        ArgumentCaptor<PointTransaction> txCaptor = ArgumentCaptor.forClass(PointTransaction.class);
        verify(pointTransactionRepository).save(txCaptor.capture());
        PointTransaction savedTx = txCaptor.getValue();
        assertThat(savedTx.getType()).isEqualTo(PointTransactionType.REDEEM);
        assertThat(savedTx.getPointsDelta()).isEqualTo(-2000);
        assertThat(savedTx.getBalanceAfter()).isEqualTo(1000);
        assertThat(savedTx.getReferenceId()).isEqualTo(99L);
    }

    @Test
    void redeemReward_shouldThrowConflictWhenInsufficientPoints() {
        Client client = Client.builder()
                .id(1L)
                .points(500)
                .lifetimePoints(500)
                .loyaltyTier(LoyaltyTier.CLASSIC)
                .build();

        Reward reward = Reward.builder()
                .id(10L)
                .name("Masaje Relajante Spa")
                .pointsCost(2000)
                .minimumTier(LoyaltyTier.CLASSIC)
                .active(true)
                .build();

        when(clientRepository.findById(1L)).thenReturn(Optional.of(client));
        when(rewardRepository.findById(10L)).thenReturn(Optional.of(reward));

        assertThatThrownBy(() -> loyaltyService.redeemReward(1L, 10L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Puntos insuficientes");

        verify(redeemedRewardRepository, never()).save(any());
        verify(pointTransactionRepository, never()).save(any());
    }

    @Test
    void redeemReward_shouldThrowConflictWhenTierRequirementNotMet() {
        Client client = Client.builder()
                .id(1L)
                .points(10000)
                .lifetimePoints(1000)
                .loyaltyTier(LoyaltyTier.CLASSIC)
                .build();

        Reward reward = Reward.builder()
                .id(11L)
                .name("Noche Suite Royale")
                .pointsCost(9000)
                .minimumTier(LoyaltyTier.IMPERIAL)
                .active(true)
                .build();

        when(clientRepository.findById(1L)).thenReturn(Optional.of(client));
        when(rewardRepository.findById(11L)).thenReturn(Optional.of(reward));

        assertThatThrownBy(() -> loyaltyService.redeemReward(1L, 11L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("requiere categoría Imperial");
    }

    @Test
    void awardPointsForReservation_shouldUpgradeTierAndBeIdempotent() {
        Client client = Client.builder()
                .id(2L)
                .firstName("Ana")
                .points(0)
                .lifetimePoints(0)
                .loyaltyTier(LoyaltyTier.CLASSIC)
                .build();

        Reservation reservation = Reservation.builder()
                .id(55L)
                .client(client)
                .build();

        when(pointTransactionRepository.existsByClientIdAndReferenceTypeAndReferenceId(
                2L, LoyaltyServiceImpl.REF_RESERVATION, 55L))
                .thenReturn(false, true);
        when(clientRepository.findById(2L)).thenReturn(Optional.of(client));
        when(pointTransactionRepository.save(any(PointTransaction.class))).thenAnswer(inv -> inv.getArgument(0));

        // $300 * 10 pts/$ * 1.0x = 3000 pts => sube a PRIVILEGE (umbral 2500)
        PointTransactionDTO first = loyaltyService.awardPointsForReservation(reservation, 300.0);
        PointTransactionDTO duplicate = loyaltyService.awardPointsForReservation(reservation, 300.0);

        assertThat(first).isNotNull();
        assertThat(first.getPointsDelta()).isEqualTo(3000);
        assertThat(client.getPoints()).isEqualTo(3000);
        assertThat(client.getLifetimePoints()).isEqualTo(3000);
        assertThat(client.getLoyaltyTier()).isEqualTo(LoyaltyTier.PRIVILEGE);
        assertThat(duplicate).isNull();
    }

    @Test
    void adjustPoints_shouldAddAndSubtractWithValidation() {
        Client client = Client.builder()
                .id(3L)
                .points(500)
                .lifetimePoints(500)
                .loyaltyTier(LoyaltyTier.CLASSIC)
                .build();

        when(clientRepository.findById(3L)).thenReturn(Optional.of(client));
        when(pointTransactionRepository.save(any(PointTransaction.class))).thenAnswer(inv -> inv.getArgument(0));

        PointTransactionDTO added = loyaltyService.adjustPoints(3L, PointAdjustmentRequestDTO.builder()
                .points(300)
                .operation("ADD")
                .description("Cortesía")
                .build());

        assertThat(added.getType()).isEqualTo("ADJUSTMENT_ADD");
        assertThat(added.getPointsDelta()).isEqualTo(300);
        assertThat(client.getPoints()).isEqualTo(800);

        PointTransactionDTO subtracted = loyaltyService.adjustPoints(3L, PointAdjustmentRequestDTO.builder()
                .points(200)
                .operation("SUBTRACT")
                .description("Corrección")
                .build());

        assertThat(subtracted.getType()).isEqualTo("ADJUSTMENT_SUBTRACT");
        assertThat(subtracted.getPointsDelta()).isEqualTo(-200);
        assertThat(client.getPoints()).isEqualTo(600);

        assertThatThrownBy(() -> loyaltyService.adjustPoints(3L, PointAdjustmentRequestDTO.builder()
                .points(900)
                .operation("SUBTRACT")
                .build()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No se pueden restar");
    }

    @Test
    void getClientLoyaltySummary_shouldComputeProgressAndTotals() {
        Client client = Client.builder()
                .id(4L)
                .firstName("Laura")
                .lastName("Gómez")
                .points(1250)
                .lifetimePoints(1250)
                .loyaltyTier(LoyaltyTier.CLASSIC)
                .build();

        when(clientRepository.findById(4L)).thenReturn(Optional.of(client));
        when(pointTransactionRepository.findByClientIdOrderByCreatedAtDesc(4L)).thenReturn(List.of(
                PointTransaction.builder().pointsDelta(1500).build(),
                PointTransaction.builder().pointsDelta(-250).build()
        ));
        when(redeemedRewardRepository.countByClientId(4L)).thenReturn(1L);

        LoyaltySummaryDTO summary = loyaltyService.getClientLoyaltySummary(4L);

        assertThat(summary.getTier()).isEqualTo("CLASSIC");
        assertThat(summary.getNextTier()).isEqualTo("PRIVILEGE");
        assertThat(summary.getPointsToNextTier()).isEqualTo(1250);
        assertThat(summary.getProgressPercentage()).isEqualTo(50);
        assertThat(summary.getTotalEarned()).isEqualTo(1500);
        assertThat(summary.getTotalSpent()).isEqualTo(250);
        assertThat(summary.getRedeemedCount()).isEqualTo(1L);
    }
}

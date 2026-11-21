package com.example.demo.service;

import com.example.demo.dto.LoyaltySummaryDTO;
import com.example.demo.dto.PointAdjustmentRequestDTO;
import com.example.demo.dto.PointTransactionDTO;
import com.example.demo.dto.RedeemedRewardDTO;
import com.example.demo.entities.Client;
import com.example.demo.entities.LoyaltyTier;
import com.example.demo.entities.PointTransaction;
import com.example.demo.entities.PointTransactionType;
import com.example.demo.entities.RedeemedReward;
import com.example.demo.entities.RedemptionStatus;
import com.example.demo.entities.Reservation;
import com.example.demo.entities.Reward;
import com.example.demo.errors.NotFoundException;
import com.example.demo.mapper.LoyaltyMapper;
import com.example.demo.repository.ClientRepository;
import com.example.demo.repository.PointTransactionRepository;
import com.example.demo.repository.RedeemedRewardRepository;
import com.example.demo.repository.RewardRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Implementación del sistema de fidelidad MontClair Privilège.
 * Garantiza atomicidad transaccional al acreditar puntos, restar puntos por canje,
 * actualizar categorías y registrar cada movimiento en el historial de transacciones.
 */
@Service
public class LoyaltyServiceImpl implements LoyaltyService {

    public static final int WELCOME_BONUS_POINTS = 250;
    public static final int BASE_POINTS_PER_DOLLAR = 10;
    public static final int MIN_RESERVATION_POINTS = 100;

    public static final String REF_RESERVATION = "RESERVATION";
    public static final String REF_REWARD_REDEMPTION = "REWARD_REDEMPTION";
    public static final String REF_WELCOME_BONUS = "WELCOME_BONUS";
    public static final String REF_MANUAL_ADJUSTMENT = "MANUAL_ADJUSTMENT";

    private final ClientRepository clientRepository;
    private final RewardRepository rewardRepository;
    private final RedeemedRewardRepository redeemedRewardRepository;
    private final PointTransactionRepository pointTransactionRepository;

    public LoyaltyServiceImpl(
            ClientRepository clientRepository,
            RewardRepository rewardRepository,
            RedeemedRewardRepository redeemedRewardRepository,
            PointTransactionRepository pointTransactionRepository) {
        this.clientRepository = clientRepository;
        this.rewardRepository = rewardRepository;
        this.redeemedRewardRepository = redeemedRewardRepository;
        this.pointTransactionRepository = pointTransactionRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public LoyaltySummaryDTO getClientLoyaltySummary(Long clientId) {
        Client client = findClientOrThrow(clientId);
        int points = safeInt(client.getPoints());
        int lifetimePoints = safeInt(client.getLifetimePoints());
        LoyaltyTier currentTier = client.getLoyaltyTier() != null
                ? client.getLoyaltyTier()
                : LoyaltyTier.fromLifetimePoints(lifetimePoints);
        LoyaltyTier nextTier = currentTier.nextTier();

        int pointsToNext = currentTier.pointsToNextTier(lifetimePoints);
        int progressPct = calculateProgressPercentage(currentTier, nextTier, lifetimePoints);

        List<PointTransaction> transactions = pointTransactionRepository.findByClientIdOrderByCreatedAtDesc(clientId);
        int totalEarned = transactions.stream()
                .filter(tx -> tx.getPointsDelta() != null && tx.getPointsDelta() > 0)
                .mapToInt(PointTransaction::getPointsDelta)
                .sum();
        int totalSpent = transactions.stream()
                .filter(tx -> tx.getPointsDelta() != null && tx.getPointsDelta() < 0)
                .mapToInt(tx -> Math.abs(tx.getPointsDelta()))
                .sum();
        long redeemedCount = redeemedRewardRepository.countByClientId(clientId);

        return LoyaltySummaryDTO.builder()
                .clientId(client.getId())
                .clientName(LoyaltyMapper.formatFullName(client))
                .points(points)
                .lifetimePoints(lifetimePoints)
                .tier(currentTier.name())
                .tierLabel(currentTier.getDisplayName())
                .pointsMultiplier(currentTier.getPointsMultiplier())
                .nextTier(nextTier != null ? nextTier.name() : null)
                .nextTierLabel(nextTier != null ? nextTier.getDisplayName() : null)
                .pointsToNextTier(pointsToNext)
                .progressPercentage(progressPct)
                .totalEarned(totalEarned)
                .totalSpent(totalSpent)
                .redeemedCount(redeemedCount)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PointTransactionDTO> getTransactionsByClient(Long clientId, PointTransactionType type) {
        findClientOrThrow(clientId);
        List<PointTransaction> list = (type != null)
                ? pointTransactionRepository.findByClientIdAndTypeOrderByCreatedAtDesc(clientId, type)
                : pointTransactionRepository.findByClientIdOrderByCreatedAtDesc(clientId);
        return list.stream()
                .map(LoyaltyMapper::toPointTransactionDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<RedeemedRewardDTO> getRedeemedRewardsByClient(Long clientId) {
        findClientOrThrow(clientId);
        return redeemedRewardRepository.findByClientIdOrderByRedeemedAtDesc(clientId).stream()
                .map(LoyaltyMapper::toRedeemedRewardDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<RedeemedRewardDTO> getAllRedeemedRewards() {
        return redeemedRewardRepository.findAllByOrderByRedeemedAtDesc().stream()
                .map(LoyaltyMapper::toRedeemedRewardDTO)
                .toList();
    }

    @Override
    @Transactional
    public RedeemedRewardDTO redeemReward(Long clientId, Long rewardId) {
        if (rewardId == null) {
            throw new IllegalArgumentException("El ID de la recompensa es obligatorio.");
        }

        Client client = findClientOrThrow(clientId);
        Reward reward = rewardRepository.findById(rewardId)
                .orElseThrow(() -> new NotFoundException(rewardId));

        if (!reward.isActive()) {
            throw new IllegalStateException("La recompensa '" + reward.getName() + "' no se encuentra activa en el catálogo.");
        }

        if (!reward.hasAvailableStock()) {
            throw new IllegalStateException("La recompensa '" + reward.getName() + "' se encuentra agotada temporalmente.");
        }

        LoyaltyTier clientTier = client.getLoyaltyTier() != null
                ? client.getLoyaltyTier()
                : LoyaltyTier.fromLifetimePoints(safeInt(client.getLifetimePoints()));
        LoyaltyTier requiredTier = reward.getMinimumTier() != null ? reward.getMinimumTier() : LoyaltyTier.CLASSIC;

        if (!clientTier.meetsRequirement(requiredTier)) {
            throw new IllegalStateException(
                    "Esta recompensa requiere categoría " + requiredTier.getDisplayName()
                            + " o superior. Tu categoría actual es " + clientTier.getDisplayName() + ".");
        }

        int currentPoints = safeInt(client.getPoints());
        int cost = safeInt(reward.getPointsCost());
        if (currentPoints < cost) {
            throw new IllegalStateException(
                    "Puntos insuficientes para canjear '" + reward.getName()
                            + "'. Necesitas " + cost + " pts y tienes " + currentPoints + " pts disponibles.");
        }

        // 1. Descontar puntos del saldo disponible (lifetimePoints se conserva para mantener la categoría)
        int newBalance = currentPoints - cost;
        client.setPoints(newBalance);
        clientRepository.save(client);

        // 2. Descontar stock si la recompensa tiene cupo limitado
        if (reward.getStock() != null) {
            reward.setStock(reward.getStock() - 1);
            rewardRepository.save(reward);
        }

        // 3. Registrar el canje en "Canjeados"
        RedeemedReward redeemed = RedeemedReward.builder()
                .client(client)
                .reward(reward)
                .pointsSpent(cost)
                .redemptionCode(generateUniqueRedemptionCode())
                .status(RedemptionStatus.ACTIVE)
                .redeemedAt(LocalDateTime.now())
                .build();
        RedeemedReward savedRedemption = redeemedRewardRepository.save(redeemed);

        // 4. Registrar la transacción negativa en el historial de puntos
        PointTransaction tx = PointTransaction.builder()
                .client(client)
                .type(PointTransactionType.REDEEM)
                .pointsDelta(-cost)
                .balanceAfter(newBalance)
                .description("Canje de recompensa: " + reward.getName() + " (" + savedRedemption.getRedemptionCode() + ")")
                .referenceId(savedRedemption.getId())
                .referenceType(REF_REWARD_REDEMPTION)
                .createdAt(LocalDateTime.now())
                .build();
        pointTransactionRepository.save(tx);

        return LoyaltyMapper.toRedeemedRewardDTO(savedRedemption);
    }

    @Override
    @Transactional
    public RedeemedRewardDTO markRedemptionAsUsed(Long redeemedRewardId) {
        if (redeemedRewardId == null) {
            throw new IllegalArgumentException("El ID del canje es obligatorio.");
        }
        RedeemedReward redeemed = redeemedRewardRepository.findById(redeemedRewardId)
                .orElseThrow(() -> new NotFoundException(redeemedRewardId));

        if (redeemed.getStatus() == RedemptionStatus.USED) {
            throw new IllegalStateException("Esta recompensa ya fue marcada como utilizada.");
        }
        if (redeemed.getStatus() == RedemptionStatus.CANCELLED) {
            throw new IllegalStateException("No se puede utilizar un canje cancelado.");
        }

        redeemed.setStatus(RedemptionStatus.USED);
        redeemed.setUsedAt(LocalDateTime.now());
        return LoyaltyMapper.toRedeemedRewardDTO(redeemedRewardRepository.save(redeemed));
    }

    @Override
    @Transactional
    public PointTransactionDTO adjustPoints(Long clientId, PointAdjustmentRequestDTO request) {
        if (request == null) {
            throw new IllegalArgumentException("Los datos del ajuste de puntos son obligatorios.");
        }
        if (request.getPoints() == null || request.getPoints() <= 0) {
            throw new IllegalArgumentException("La cantidad de puntos debe ser mayor a cero.");
        }
        if (request.getOperation() == null || request.getOperation().isBlank()) {
            throw new IllegalArgumentException("Debe indicar la operación: ADD o SUBTRACT.");
        }

        String op = request.getOperation().trim().toUpperCase();
        boolean isAdd = "ADD".equals(op);
        boolean isSubtract = "SUBTRACT".equals(op);
        if (!isAdd && !isSubtract) {
            throw new IllegalArgumentException("Operación inválida. Use ADD o SUBTRACT.");
        }

        Client client = findClientOrThrow(clientId);
        int amount = request.getPoints();
        int currentPoints = safeInt(client.getPoints());
        int currentLifetime = safeInt(client.getLifetimePoints());

        int delta;
        int newBalance;
        PointTransactionType txType;

        if (isAdd) {
            delta = amount;
            newBalance = currentPoints + amount;
            int newLifetime = currentLifetime + amount;
            client.setPoints(newBalance);
            client.setLifetimePoints(newLifetime);
            client.setLoyaltyTier(LoyaltyTier.fromLifetimePoints(newLifetime));
            txType = PointTransactionType.ADJUSTMENT_ADD;
        } else {
            if (currentPoints < amount) {
                throw new IllegalStateException(
                        "No se pueden restar " + amount + " pts porque el cliente solo tiene " + currentPoints + " pts disponibles.");
            }
            delta = -amount;
            newBalance = currentPoints - amount;
            client.setPoints(newBalance);
            txType = PointTransactionType.ADJUSTMENT_SUBTRACT;
        }

        clientRepository.save(client);

        String description = (request.getDescription() != null && !request.getDescription().isBlank())
                ? request.getDescription().trim()
                : (isAdd ? "Bonificación manual de puntos" : "Ajuste / descuento manual de puntos");

        PointTransaction tx = PointTransaction.builder()
                .client(client)
                .type(txType)
                .pointsDelta(delta)
                .balanceAfter(newBalance)
                .description(description)
                .referenceType(REF_MANUAL_ADJUSTMENT)
                .createdAt(LocalDateTime.now())
                .build();

        return LoyaltyMapper.toPointTransactionDTO(pointTransactionRepository.save(tx));
    }

    @Override
    @Transactional
    public PointTransactionDTO awardPointsForReservation(Reservation reservation, double totalAmount) {
        if (reservation == null || reservation.getId() == null || reservation.getClient() == null) {
            return null;
        }

        Long clientId = reservation.getClient().getId();
        if (clientId == null) {
            return null;
        }

        // Idempotencia: no volver a acreditar puntos si esta reserva ya fue recompensada
        if (pointTransactionRepository.existsByClientIdAndReferenceTypeAndReferenceId(
                clientId, REF_RESERVATION, reservation.getId())) {
            return null;
        }

        Client client = clientRepository.findById(clientId).orElse(null);
        if (client == null) {
            return null;
        }

        LoyaltyTier currentTier = client.getLoyaltyTier() != null
                ? client.getLoyaltyTier()
                : LoyaltyTier.fromLifetimePoints(safeInt(client.getLifetimePoints()));

        int rawEarned = (int) Math.round(Math.max(0.0, totalAmount) * BASE_POINTS_PER_DOLLAR * currentTier.getPointsMultiplier());
        int pointsEarned = Math.max(MIN_RESERVATION_POINTS, rawEarned);

        int newBalance = safeInt(client.getPoints()) + pointsEarned;
        int newLifetime = safeInt(client.getLifetimePoints()) + pointsEarned;
        client.setPoints(newBalance);
        client.setLifetimePoints(newLifetime);
        client.setLoyaltyTier(LoyaltyTier.fromLifetimePoints(newLifetime));
        clientRepository.save(client);

        PointTransaction tx = PointTransaction.builder()
                .client(client)
                .type(PointTransactionType.EARN)
                .pointsDelta(pointsEarned)
                .balanceAfter(newBalance)
                .description("Puntos acumulados por pago de Reserva #" + reservation.getId()
                        + " (Multiplicador " + currentTier.getDisplayName() + " " + currentTier.getPointsMultiplier() + "x)")
                .referenceId(reservation.getId())
                .referenceType(REF_RESERVATION)
                .createdAt(LocalDateTime.now())
                .build();

        return LoyaltyMapper.toPointTransactionDTO(pointTransactionRepository.save(tx));
    }

    @Override
    @Transactional
    public PointTransactionDTO grantWelcomeBonus(Long clientId) {
        if (clientId == null) {
            return null;
        }
        if (pointTransactionRepository.existsByClientIdAndReferenceTypeAndReferenceId(
                clientId, REF_WELCOME_BONUS, clientId)) {
            return null;
        }

        Client client = clientRepository.findById(clientId).orElse(null);
        if (client == null) {
            return null;
        }

        int newBalance = safeInt(client.getPoints()) + WELCOME_BONUS_POINTS;
        int newLifetime = safeInt(client.getLifetimePoints()) + WELCOME_BONUS_POINTS;
        client.setPoints(newBalance);
        client.setLifetimePoints(newLifetime);
        client.setLoyaltyTier(LoyaltyTier.fromLifetimePoints(newLifetime));
        clientRepository.save(client);

        PointTransaction tx = PointTransaction.builder()
                .client(client)
                .type(PointTransactionType.EARN)
                .pointsDelta(WELCOME_BONUS_POINTS)
                .balanceAfter(newBalance)
                .description("Bono de bienvenida al programa MontClair Privilège")
                .referenceId(clientId)
                .referenceType(REF_WELCOME_BONUS)
                .createdAt(LocalDateTime.now())
                .build();

        return LoyaltyMapper.toPointTransactionDTO(pointTransactionRepository.save(tx));
    }

    private Client findClientOrThrow(Long clientId) {
        if (clientId == null) {
            throw new IllegalArgumentException("El ID del cliente es obligatorio.");
        }
        return clientRepository.findById(clientId)
                .orElseThrow(() -> new NotFoundException(clientId));
    }

    private int safeInt(Integer value) {
        return value != null ? value : 0;
    }

    private int calculateProgressPercentage(LoyaltyTier currentTier, LoyaltyTier nextTier, int lifetimePoints) {
        if (nextTier == null) {
            return 100;
        }
        int floor = currentTier.getMinLifetimePoints();
        int ceiling = nextTier.getMinLifetimePoints();
        if (ceiling <= floor) {
            return 100;
        }
        int clamped = Math.max(0, lifetimePoints - floor);
        return Math.min(100, (int) Math.round((clamped * 100.0) / (ceiling - floor)));
    }

    private String generateUniqueRedemptionCode() {
        String code;
        do {
            code = "MC-RWD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        } while (redeemedRewardRepository.findByRedemptionCodeIgnoreCase(code).isPresent());
        return code;
    }
}

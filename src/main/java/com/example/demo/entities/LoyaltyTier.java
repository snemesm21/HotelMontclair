package com.example.demo.entities;

/**
 * Categorías del programa de fidelidad MontClair Privilège.
 * La categoría se determina a partir de los puntos históricos acumulados (lifetimePoints).
 */
public enum LoyaltyTier {
    CLASSIC("Classic", 0, 1.0),
    PRIVILEGE("Privilège", 2500, 1.25),
    IMPERIAL("Imperial", 10000, 1.5);

    private final String displayName;
    private final int minLifetimePoints;
    private final double pointsMultiplier;

    LoyaltyTier(String displayName, int minLifetimePoints, double pointsMultiplier) {
        this.displayName = displayName;
        this.minLifetimePoints = minLifetimePoints;
        this.pointsMultiplier = pointsMultiplier;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getMinLifetimePoints() {
        return minLifetimePoints;
    }

    public double getPointsMultiplier() {
        return pointsMultiplier;
    }

    public static LoyaltyTier fromLifetimePoints(int lifetimePoints) {
        if (lifetimePoints >= IMPERIAL.minLifetimePoints) {
            return IMPERIAL;
        }
        if (lifetimePoints >= PRIVILEGE.minLifetimePoints) {
            return PRIVILEGE;
        }
        return CLASSIC;
    }

    public LoyaltyTier nextTier() {
        return switch (this) {
            case CLASSIC -> PRIVILEGE;
            case PRIVILEGE -> IMPERIAL;
            case IMPERIAL -> null;
        };
    }

    public int pointsToNextTier(int lifetimePoints) {
        LoyaltyTier next = nextTier();
        if (next == null) {
            return 0;
        }
        return Math.max(0, next.minLifetimePoints - lifetimePoints);
    }

    public boolean meetsRequirement(LoyaltyTier requiredTier) {
        if (requiredTier == null) {
            return true;
        }
        return this.ordinal() >= requiredTier.ordinal();
    }
}

package com.donutpay;

public enum Tier {
    SMALL(4200),
    MEDIUM(4800),
    LARGE(5800),
    HUGE(8500),
    UNIVERSAL(9500);

    public final long durationMs;

    Tier(long durationMs) {
        this.durationMs = durationMs;
    }

    /** Returns the tier for an amount, or null when it is below the smallest threshold. */
    public static Tier of(long amount, Config c) {
        if (amount >= c.universal) return UNIVERSAL;
        if (amount >= c.huge) return HUGE;
        if (amount >= c.large) return LARGE;
        if (amount >= c.medium) return MEDIUM;
        if (amount >= c.small) return SMALL;
        return null;
    }
}

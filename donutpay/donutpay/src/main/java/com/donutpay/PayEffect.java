package com.donutpay;

import net.minecraft.world.entity.player.PlayerSkin;

/** One payment animation. Small ones start at once; bigger ones wait their turn. */
public final class PayEffect {
    public final Tier tier;
    public final String player;
    public final long amount;
    public final PlayerSkin skin;
    public final long seed;

    /** Start time in ms, or -1 while queued. */
    public long start = -1;
    public int slot;
    private long fired;

    public PayEffect(Tier tier, String player, long amount, PlayerSkin skin, long seed) {
        this.tier = tier;
        this.player = player;
        this.amount = amount;
        this.skin = skin;
        this.seed = seed;
    }

    public boolean started() {
        return start >= 0;
    }

    /** True only the first time a given bit (0-63) is asked for. Used for one-shot sounds. */
    public boolean once(int bit) {
        long m = 1L << (bit & 63);
        if ((fired & m) != 0) return false;
        fired |= m;
        return true;
    }
}

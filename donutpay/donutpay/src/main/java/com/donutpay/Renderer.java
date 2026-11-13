package com.donutpay;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.List;
import java.util.Locale;
import java.util.Random;

import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.PlayerFaceExtractor;
import net.minecraft.util.Mth;

/**
 * Draws every animation using only fill() and text(), so no texture files are needed.
 * "u" is a screen-size factor (1.0 at 270 gui pixels high) that keeps sizes consistent at any GUI scale.
 */
public final class Renderer {
    private static final DecimalFormat FMT = new DecimalFormat("0.##", DecimalFormatSymbols.getInstance(Locale.ROOT));
    private static final int GOLD = 0xFFC72C;

    private Renderer() {}

    // ------------------------------------------------------------------ entry

    public static void render(GuiGraphicsExtractor g) {
        List<PayEffect> list = DonutPayClient.EFFECTS;
        if (list.isEmpty()) return;

        Minecraft mc = Minecraft.getInstance();
        long now = Util.getMillis();

        list.removeIf(e -> e.started() && now - e.start > e.tier.durationMs);

        boolean bigActive = false;
        for (PayEffect e : list) {
            if (e.started() && e.tier != Tier.SMALL) bigActive = true;
        }
        for (PayEffect e : list) {
            if (e.started()) continue;
            if (e.tier == Tier.SMALL) {
                e.slot = freeSlot(list);
                e.start = now;
                DonutPayClient.onBegin(e);
            } else if (!bigActive) {
                e.start = now;
                bigActive = true;
                DonutPayClient.onBegin(e);
            }
        }

        int w = mc.getWindow().getGuiScaledWidth();
        int h = mc.getWindow().getGuiScaledHeight();
        for (PayEffect e : list) {
            if (!e.started()) continue;
            float t = (now - e.start) / 1000f;
            switch (e.tier) {
                case SMALL -> small(g, mc, e, t, w, h);
                case MEDIUM -> fireworks(g, mc, e, t, w, h);
                case LARGE -> coins(g, mc, e, t, w, h);
                case HUGE -> nuke(g, mc, e, t, w, h);
                case UNIVERSAL -> galaxy(g, mc, e, t, w, h);
            }
        }
    }

    private static int freeSlot(List<PayEffect> list) {
        int slot = 0;
        while (true) {
            boolean used = false;
            for (PayEffect o : list) {
                if (o.started() && o.tier == Tier.SMALL && o.slot == slot) used = true;
            }
            if (!used) return slot;
            slot++;
        }
    }

    // ------------------------------------------------------------------ SMALL: right-side popup

    private static void small(GuiGraphicsExtractor g, Minecraft mc, PayEffect e, float t, int w, int h) {
        float in = 0.35f, hold = 3.5f, out = 0.35f;
        float cw = 160, ch = 40;
        float rest = w - cw - 6;

        float x;
        if (t < in) x = lerp(w + 4, rest, easeOut(t / in));
        else if (t < in + hold) x = rest;
        else x = lerp(rest, w + 4, easeIn(clamp01((t - in - hold) / out)));
        float y = h / 2f - ch - 10 + e.slot * (ch + 5);

        int xi = Math.round(x), yi = Math.round(y);
        rect(g, x, y, cw, ch, 0xE0121218);
        rect(g, x, y, 3, ch, 0xFF000000 | GOLD);
        rect(g, x, y, cw, 1, 0x55FFFFFF);
        rect(g, x, y + ch - 1, cw, 1, 0x55FFFFFF);
        rect(g, x + cw - 1, y, 1, ch, 0x55FFFFFF);

        PlayerFaceExtractor.extractRenderState(g, e.skin, xi + 8, yi + 6, 28);
        g.text(mc.font, e.player, xi + 42, yi + 8, 0xFFFFFFFF, true);
        g.text(mc.font, "+" + money(e.amount), xi + 42, yi + 22, 0xFF000000 | GOLD, true);
    }

    // ------------------------------------------------------------------ MEDIUM: fireworks

    private static void fireworks(GuiGraphicsExtractor g, Minecraft mc, PayEffect e, float t, int w, int h) {
        float u = h / 270f;
        float cx = w / 2f, cy = h / 2f;
        float dur = e.tier.durationMs / 1000f;
        int[] palette = {0xFF5555, 0xFFAA00, 0xFFFF55, 0x55FF55, 0x55FFFF, 0xFF55FF, 0xFFFFFF};

        for (int b = 0; b < 6; b++) {
            Random r = new Random(e.seed * 31 + b);
            float launch = b * 0.5f;
            float bx = cx + (r.nextFloat() - 0.5f) * 220 * u;
            float by = cy - 25 * u + (r.nextFloat() - 0.5f) * 90 * u;
            float bt = t - launch;
            if (bt < 0) continue;

            float rise = 0.5f;
            if (bt < rise) {
                float p = easeOut(bt / rise);
                float ry = h + (by - h) * p;
                rect(g, bx - u, ry, 2 * u, 10 * u, 0xFFFFE9A8);
                rect(g, bx - u * 0.5f, ry + 10 * u, u, 16 * u * (1 - p), 0x66FFAA33);
                continue;
            }

            if (e.once(b)) {
                DonutPayClient.play("entity.firework_rocket.large_blast", 0.9f + b * 0.04f, 0.8f);
            }

            float age = bt - rise;
            float life = 1.7f;
            if (age > life) continue;

            int c1 = palette[r.nextInt(palette.length)];
            int c2 = palette[r.nextInt(palette.length)];
            int n = 44;
            for (int i = 0; i < n; i++) {
                float ang = (float) (i * 2 * Math.PI / n) + r.nextFloat() * 0.25f;
                float sp = (55 + r.nextFloat() * 85) * u;
                float drag = 1f - 0.35f * (age / life);
                float px = bx + (float) Math.cos(ang) * sp * age * drag;
                float py = by + (float) Math.sin(ang) * sp * age * drag + 0.5f * 85 * u * age * age;
                float f = 1f - age / life;
                int rgb = (i % 2 == 0) ? c1 : c2;
                float s = Math.max(1.5f, 2.2f * u);
                rect(g, px, py, s, s, argb(alpha(f), rgb));
                // short trail
                float a2 = Math.max(0, age - 0.07f);
                float tx = bx + (float) Math.cos(ang) * sp * a2 * drag;
                float ty = by + (float) Math.sin(ang) * sp * a2 * drag + 0.5f * 85 * u * a2 * a2;
                rect(g, tx, ty, s * 0.7f, s * 0.7f, argb(alpha(f * 0.45f), rgb));
            }
            if (age < 0.12f) {
                disc(g, bx, by, 14 * u * (1 - age / 0.12f), argb(alpha(0.8f), 0xFFFFFF));
            }
        }

        float fade = window(t, 0.35f, dur, 0.8f);
        banner(g, mc, e, u, cx, cy + 40 * u, fade, "MEDIUM PAYMENT", 0xFFAA00);
    }

    // ------------------------------------------------------------------ LARGE: falling gold coins

    private static void coins(GuiGraphicsExtractor g, Minecraft mc, PayEffect e, float t, int w, int h) {
        float u = h / 270f;
        float cx = w / 2f;
        float dur = e.tier.durationMs / 1000f;
        float fade = window(t, 0.3f, dur, 0.9f);

        // coin tinkle, one bucket every 0.3s for the first 3.5s
        int bucket = (int) (t / 0.3f);
        if (bucket < 12 && e.once(bucket)) {
            DonutPayClient.play("entity.experience_orb.pickup", 1.4f + (bucket % 3) * 0.2f, 0.5f);
        }

        Random r = new Random(e.seed);
        int n = 90;
        for (int i = 0; i < n; i++) {
            float delay = r.nextFloat() * 3.0f;
            float x = cx + (r.nextFloat() - 0.5f) * w * 0.5f;
            float speed = (110 + r.nextFloat() * 120) * u;
            float size = (7 + r.nextFloat() * 7) * u;
            float spin = 4 + r.nextFloat() * 6;
            float phase = r.nextFloat() * 6.28f;

            float ct = t - delay;
            if (ct < 0) continue;
            float y = -size + ct * speed;
            if (y > h + size) continue;

            float wob = (float) Math.abs(Math.cos(phase + ct * spin));
            float cw = Math.max(1f, size * wob);
            float cyc = y + size / 2f;
            ellipse(g, x, cyc, cw / 2f, size / 2f, argb(alpha(fade), 0xB8860B));
            if (cw > 3) {
                ellipse(g, x, cyc, cw / 2f - 1, size / 2f - 1, argb(alpha(fade), 0xFFD700));
                ellipse(g, x - cw * 0.12f, cyc - size * 0.12f, cw * 0.18f, size * 0.18f, argb(alpha(fade), 0xFFF5A0));
            }
        }

        banner(g, mc, e, u, cx, h / 2f - 28 * u, fade, "LARGE PAYMENT", GOLD);
    }

    // ------------------------------------------------------------------ HUGE: nuke

    private static void nuke(GuiGraphicsExtractor g, Minecraft mc, PayEffect e, float t, int w, int h) {
        float u = h / 270f;
        float dur = e.tier.durationMs / 1000f;
        float fa = clamp01((dur - t) / 1.2f);
        float impactT = 1.3f;
        float cx = w / 2f;
        float iy = h / 2f + 60 * u;

        if (t < impactT) {
            float pulse = 0.5f + 0.5f * (float) Math.sin(t * 14);
            int edge = argb(alpha(0.55f * pulse), 0xFF0000);
            float th = 16 * u;
            rect(g, 0, 0, w, th, edge);
            rect(g, 0, h - th, w, th, edge);
            rect(g, 0, 0, th, h, edge);
            rect(g, w - th, 0, th, h, edge);
            if (((int) (t * 6)) % 2 == 0) {
                text(g, mc, "NUKE INCOMING", cx, h * 0.14f, 2.4f * u, 0xFFFF3030);
            }

            float p = t / impactT;
            float my = -30 * u + (iy + 34 * u) * (p * p);
            float flick = 0.7f + 0.3f * (float) Math.sin(t * 40);
            rect(g, cx - u, 0, 2 * u, Math.max(0, my - 26 * u), 0x55AAAAAA);          // smoke trail
            rect(g, cx - 2 * u, my - 26 * u - 14 * u * flick, 4 * u, 14 * u * flick, 0xFFFF8A1F); // flame
            rect(g, cx - u, my - 26 * u - 8 * u * flick, 2 * u, 8 * u * flick, 0xFFFFE070);
            rect(g, cx - 3 * u, my - 26 * u, 6 * u, 26 * u, 0xFFD8D8D8);              // body
            rect(g, cx - 6 * u, my - 26 * u, 3 * u, 9 * u, 0xFF8A8A8A);               // fins
            rect(g, cx + 3 * u, my - 26 * u, 3 * u, 9 * u, 0xFF8A8A8A);
            rect(g, cx - 2 * u, my, 4 * u, 4 * u, 0xFFCC2222);                         // nose
            rect(g, cx - u, my + 4 * u, 2 * u, 3 * u, 0xFFCC2222);
            return;
        }

        float tt = t - impactT;
        if (e.once(0)) {
            DonutPayClient.play("entity.generic.explode", 0.6f, 1.0f);
        }

        float shake = 6 * u * Math.max(0f, 1f - tt / 2.2f);
        var pose = g.pose();
        pose.pushMatrix();
        if (shake > 0.1f) {
            long ms = Util.getMillis();
            pose.translate((float) Math.sin(ms * 0.09) * shake, (float) Math.cos(ms * 0.13) * shake);
        }

        float eFire = easeOut(Math.min(tt / 1.2f, 1f));
        float eStem = easeOut(Math.min(tt / 1.8f, 1f));
        float eCap = easeOut(Math.min(tt / 2.4f, 1f));
        float cool = clamp01((tt - 1.5f) / 3.5f);

        // shockwave rings
        for (int k = 0; k < 2; k++) {
            float st = tt - k * 0.25f;
            if (st < 0 || st > 1.3f) continue;
            float rr = st * 230 * u;
            float a = (1f - st / 1.3f) * 0.8f * fa;
            ellipseRing(g, cx, iy, rr, rr * 0.22f, 3 * u, argb(alpha(a), 0xFFE8B0));
        }

        // dust skirt
        ellipse(g, cx, iy, (30 + 55 * eCap) * u, (5 + 5 * eCap) * u,
                argb(alpha(0.8f * fa), lerpRgb(0x7A4A1E, 0x4A4A4A, cool)));

        // fireball
        if (tt < 2.2f) {
            float fr = (12 + 40 * eFire) * u;
            float a = Math.max(0f, 1f - tt / 2.2f) * fa;
            disc(g, cx, iy - fr * 0.4f, fr, argb(alpha(a), 0xFF5A10));
            disc(g, cx, iy - fr * 0.4f, fr * 0.72f, argb(alpha(a), 0xFF9A1E));
            disc(g, cx, iy - fr * 0.4f, fr * 0.45f, argb(alpha(a), 0xFFE070));
            disc(g, cx, iy - fr * 0.4f, fr * 0.22f, argb(alpha(a), 0xFFFFE8));
        }

        // stem
        float stemH = 78 * u * eStem;
        float stemW = (9 + 7 * eStem) * u;
        rect(g, cx - stemW / 2, iy - stemH, stemW, stemH, argb(alpha(fa), lerpRgb(0xE0701A, 0x595959, cool)));
        rect(g, cx - stemW / 4, iy - stemH, stemW / 2, stemH, argb(alpha(fa), lerpRgb(0xFFC23A, 0x7A7A7A, cool)));

        // cap
        float capY = iy - stemH - 4 * u * eCap;
        float rx = (14 + 42 * eCap) * u;
        float ry = (10 + 24 * eCap) * u;
        ellipse(g, cx, capY, rx, ry, argb(alpha(fa), lerpRgb(0x8A3A10, 0x3A3A3A, cool)));
        ellipse(g, cx, capY - ry * 0.08f, rx * 0.84f, ry * 0.82f, argb(alpha(fa), lerpRgb(0xE0701A, 0x555555, cool)));
        ellipse(g, cx, capY - ry * 0.18f, rx * 0.55f, ry * 0.55f, argb(alpha(fa), lerpRgb(0xFFB02E, 0x6E6E6E, cool)));

        pose.popMatrix();

        float flash = Math.max(0f, 1f - tt / 0.7f);
        if (flash > 0) rect(g, 0, 0, w, h, argb(alpha(0.95f * flash), 0xFFFFFF));

        float fade = clamp01((tt - 0.6f) / 0.5f) * fa;
        banner(g, mc, e, u, cx, h * 0.68f, fade, "HUGE PAYMENT", 0xFF5533);
    }

    // ------------------------------------------------------------------ UNIVERSAL: galaxy

    private static void galaxy(GuiGraphicsExtractor g, Minecraft mc, PayEffect e, float t, int w, int h) {
        float u = h / 270f;
        float dur = e.tier.durationMs / 1000f;
        float fade = window(t, 1.0f, dur, 1.2f);
        float cx = w / 2f, cy = h / 2f - 8 * u;

        if (t > 1.0f && e.once(0)) {
            DonutPayClient.play("block.amethyst_block.resonate", 0.6f, 1.0f);
        }

        rect(g, 0, 0, w, h, argb(alpha(0.6f * fade), 0x02010A));

        // background stars
        Random rb = new Random(e.seed);
        for (int i = 0; i < 110; i++) {
            float x = rb.nextFloat() * w, y = rb.nextFloat() * h;
            float tw = 0.5f + 0.5f * (float) Math.sin(t * (1.5f + rb.nextFloat() * 3f) + i);
            float s = Math.max(1f, (rb.nextFloat() < 0.15f ? 2f : 1f) * u * 0.8f);
            rect(g, x, y, s, s, argb(alpha(tw * fade * 0.9f), 0xCFE0FF));
        }

        // spiral arms
        float radius = 112 * u;
        int arms = 3, per = 150;
        float rot = t * 0.45f;
        Random rs = new Random(e.seed * 7 + 1);
        for (int a = 0; a < arms; a++) {
            for (int i = 0; i < per; i++) {
                float f = (i + 1f) / per;
                float rad = (float) Math.pow(f, 0.85) * radius;
                float ang = a * (float) (2 * Math.PI / arms) + f * 3.4f + rot + (rs.nextFloat() - 0.5f) * 0.45f * (0.3f + f);
                float spread = (rs.nextFloat() - 0.5f) * 10 * u * f;
                float x = cx + (float) Math.cos(ang) * rad + spread;
                float y = cy + (float) Math.sin(ang) * rad * 0.5f + spread * 0.5f;
                int rgb = lerpRgb(0xFFF1C1, (a % 2 == 0) ? 0x6C7BFF : 0xB05CFF, f);
                float tw = 0.65f + 0.35f * (float) Math.sin(t * 3 + i * 1.7f + a);
                float s = Math.max(1f, ((rs.nextFloat() < 0.12f) ? 2.2f : 1.2f) * u * 0.9f);
                rect(g, x, y, s, s, argb(alpha(tw * fade * (1f - 0.35f * f)), rgb));
            }
        }

        // glowing core
        float pulse = 1f + 0.08f * (float) Math.sin(t * 2.5f);
        for (int k = 0; k < 6; k++) {
            disc(g, cx, cy, (26 - 4 * k) * u * pulse, argb(alpha((0.07f + k * 0.05f) * fade), 0xFFE9B0));
        }

        int labelRgb = Mth.hsvToRgb((t * 0.25f) % 1f, 0.5f, 1f);
        banner(g, mc, e, u, cx, cy + 62 * u, fade, "UNIVERSAL PAYMENT", labelRgb);
    }

    // ------------------------------------------------------------------ shared pieces

    /** Tier label, player name and amount on a dark panel. */
    private static void banner(GuiGraphicsExtractor g, Minecraft mc, PayEffect e, float u, float cx, float y,
                               float fade, String label, int labelRgb) {
        if (fade <= 0.02f) return;
        String name = e.player;
        String amt = money(e.amount);
        float ls = 1.1f * u, ns = 2.0f * u, as = 2.6f * u;

        float wName = mc.font.width(name) * ns;
        float wAmt = mc.font.width(amt) * as;
        float wLab = mc.font.width(label) * ls;
        float pw = Math.max(wName, Math.max(wAmt, wLab)) + 24 * u;
        float ph = 9 * ls + 9 * ns + 9 * as + 14 * u;

        rect(g, cx - pw / 2, y - 5 * u, pw, ph, argb(alpha(0.55f * fade), 0x000000));
        float yy = y;
        text(g, mc, label, cx, yy, ls, argb(alpha(fade), labelRgb));
        yy += 9 * ls + 2 * u;
        text(g, mc, name, cx, yy, ns, argb(alpha(fade), 0xFFFFFF));
        yy += 9 * ns + 2 * u;
        text(g, mc, amt, cx, yy, as, argb(alpha(fade), GOLD));
    }

    private static void text(GuiGraphicsExtractor g, Minecraft mc, String s, float cx, float y, float scale, int argb) {
        var pose = g.pose();
        pose.pushMatrix();
        pose.translate(cx, y);
        pose.scale(scale, scale);
        // text with a near-zero alpha is drawn opaque by vanilla, so keep it above that
        int a = (argb >>> 24) & 0xFF;
        if (a < 5) a = 5;
        g.text(mc.font, s, -mc.font.width(s) / 2, 0, (a << 24) | (argb & 0xFFFFFF), true);
        pose.popMatrix();
    }

    public static String money(long v) {
        double d = v;
        if (d >= 1e12) return "$" + FMT.format(d / 1e12) + "T";
        if (d >= 1e9) return "$" + FMT.format(d / 1e9) + "B";
        if (d >= 1e6) return "$" + FMT.format(d / 1e6) + "M";
        if (d >= 1e3) return "$" + FMT.format(d / 1e3) + "K";
        return "$" + v;
    }

    // ------------------------------------------------------------------ drawing helpers

    private static void rect(GuiGraphicsExtractor g, float x, float y, float w, float h, int color) {
        int x1 = Math.round(x), y1 = Math.round(y);
        int x2 = Math.max(x1 + 1, Math.round(x + w));
        int y2 = Math.max(y1 + 1, Math.round(y + h));
        g.fill(x1, y1, x2, y2, color);
    }

    private static void disc(GuiGraphicsExtractor g, float cx, float cy, float r, int color) {
        ellipse(g, cx, cy, r, r, color);
    }

    private static void ellipse(GuiGraphicsExtractor g, float cx, float cy, float rx, float ry, int color) {
        if (rx < 0.5f || ry < 0.5f) return;
        int cxi = Math.round(cx), cyi = Math.round(cy);
        int n = (int) Math.ceil(ry);
        for (int dy = -n; dy <= n; dy++) {
            float q = 1f - (dy * dy) / (ry * ry);
            if (q <= 0) continue;
            int hw = (int) (rx * Math.sqrt(q));
            if (hw <= 0) continue;
            g.fill(cxi - hw, cyi + dy, cxi + hw, cyi + dy + 1, color);
        }
    }

    private static void ellipseRing(GuiGraphicsExtractor g, float cx, float cy, float rx, float ry, float thick, int color) {
        if (rx < 1f || ry < 1f) return;
        int cxi = Math.round(cx), cyi = Math.round(cy);
        float irx = Math.max(0, rx - thick), iry = Math.max(0.5f, ry - thick * 0.3f);
        int n = (int) Math.ceil(ry);
        for (int dy = -n; dy <= n; dy++) {
            float q = 1f - (dy * dy) / (ry * ry);
            if (q <= 0) continue;
            int ox = (int) (rx * Math.sqrt(q));
            int ix = 0;
            float qi = 1f - (dy * dy) / (iry * iry);
            if (qi > 0) ix = (int) (irx * Math.sqrt(qi));
            if (ox <= ix) continue;
            g.fill(cxi - ox, cyi + dy, cxi - ix, cyi + dy + 1, color);
            g.fill(cxi + ix, cyi + dy, cxi + ox, cyi + dy + 1, color);
        }
    }

    // ------------------------------------------------------------------ math helpers

    private static int alpha(float f) {
        return Math.max(0, Math.min(255, (int) (f * 255)));
    }

    private static int argb(int a, int rgb) {
        return (Math.max(0, Math.min(255, a)) << 24) | (rgb & 0xFFFFFF);
    }

    private static float clamp01(float f) {
        return Math.max(0f, Math.min(1f, f));
    }

    private static float lerp(float a, float b, float f) {
        return a + (b - a) * f;
    }

    private static float easeOut(float p) {
        float q = 1f - clamp01(p);
        return 1f - q * q * q;
    }

    private static float easeIn(float p) {
        float q = clamp01(p);
        return q * q * q;
    }

    private static int lerpRgb(int a, int b, float f) {
        f = clamp01(f);
        int r = (int) lerp((a >> 16) & 0xFF, (b >> 16) & 0xFF, f);
        int gg = (int) lerp((a >> 8) & 0xFF, (b >> 8) & 0xFF, f);
        int bl = (int) lerp(a & 0xFF, b & 0xFF, f);
        return (r << 16) | (gg << 8) | bl;
    }

    /** Fade in over "in" seconds, fade out over the last "out" seconds of "total". */
    private static float window(float t, float in, float total, float out) {
        return clamp01(Math.min(t / in, (total - t) / out));
    }
}

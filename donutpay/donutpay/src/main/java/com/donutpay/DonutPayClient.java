package com.donutpay;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mojang.brigadier.arguments.StringArgumentType;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.player.PlayerSkin;

public class DonutPayClient implements ClientModInitializer {
    public static final String MOD_ID = "donutpay";
    public static final Logger LOG = LoggerFactory.getLogger(MOD_ID);

    public static Config config = new Config();
    public static final List<PayEffect> EFFECTS = new ArrayList<>();

    @Override
    public void onInitializeClient() {
        config = Config.load();

        // Payment messages from the server arrive as system ("game") messages.
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (!overlay) handle(message.getString());
        });

        HudElementRegistry.attachElementBefore(
                VanillaHudElements.CHAT,
                Identifier.fromNamespaceAndPath(MOD_ID, "payments"),
                (graphics, deltaTracker) -> Renderer.render(graphics));

        // /donutpay test <player> <amount>   e.g. /donutpay test Steve 5M
        // /donutpay reload
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
                dispatcher.register(ClientCommands.literal("donutpay")
                        .then(ClientCommands.literal("reload").executes(c -> {
                            config = Config.load();
                            c.getSource().sendFeedback(Component.literal("DonutPay config reloaded"));
                            return 1;
                        }))
                        .then(ClientCommands.literal("test")
                                .then(ClientCommands.argument("player", StringArgumentType.word())
                                        .then(ClientCommands.argument("amount", StringArgumentType.word())
                                                .executes(c -> {
                                                    String player = StringArgumentType.getString(c, "player");
                                                    long amount = parseAmount(StringArgumentType.getString(c, "amount"));
                                                    if (amount < 0) {
                                                        c.getSource().sendFeedback(Component.literal(
                                                                "Could not read that amount. Try 250K, 5M or 1500000"));
                                                        return 0;
                                                    }
                                                    trigger(player, amount);
                                                    return 1;
                                                }))))));
    }

    private static void handle(String text) {
        if (!config.enabled) return;
        if (config.logSystemMessages) LOG.info("[system message] {}", text);

        Matcher m = config.pattern().matcher(text);
        if (!m.find()) return;

        long amount = parseAmount(m.group("amount"));
        if (amount < 0) return;
        trigger(m.group("player"), amount);
    }

    public static void trigger(String player, long amount) {
        Tier tier = Tier.of(amount, config);
        if (tier == null) return;

        Minecraft mc = Minecraft.getInstance();
        mc.execute(() -> {
            PlayerInfo info = mc.getConnection() == null ? null : mc.getConnection().getPlayerInfo(player);
            PlayerSkin skin = info != null ? info.getSkin() : DefaultPlayerSkin.getDefaultSkin();
            EFFECTS.add(new PayEffect(tier, player, amount, skin, System.nanoTime()));
        });
    }

    /** Called by the renderer when an effect actually starts playing. */
    static void onBegin(PayEffect e) {
        switch (e.tier) {
            case SMALL -> play("entity.experience_orb.pickup", 1.3f, 0.9f);
            case MEDIUM -> play("entity.firework_rocket.launch", 1.0f, 1.0f);
            case LARGE -> play("ui.toast.challenge_complete", 1.0f, 0.8f);
            case HUGE -> play("entity.tnt.primed", 0.8f, 1.0f);
            case UNIVERSAL -> play("block.beacon.activate", 0.8f, 1.0f);
        }
    }

    static void play(String soundId, float pitch, float volume) {
        if (!config.sounds) return;
        SoundEvent event = SoundEvent.createVariableRangeEvent(Identifier.withDefaultNamespace(soundId));
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(event, pitch, volume));
    }

    /** Reads "$250K", "1.5M", "2,500,000" and similar. Returns -1 if it cannot be read. */
    public static long parseAmount(String raw) {
        if (raw == null) return -1;
        String s = raw.replace("$", "").replace(",", "").replace(" ", "").trim().toUpperCase(Locale.ROOT);
        if (s.isEmpty()) return -1;

        double mult = 1;
        char last = s.charAt(s.length() - 1);
        if (Character.isLetter(last)) {
            switch (last) {
                case 'K' -> mult = 1e3;
                case 'M' -> mult = 1e6;
                case 'B' -> mult = 1e9;
                case 'T' -> mult = 1e12;
                default -> {
                    return -1;
                }
            }
            s = s.substring(0, s.length() - 1);
        }
        try {
            return Math.round(Double.parseDouble(s) * mult);
        } catch (NumberFormatException ex) {
            return -1;
        }
    }
}

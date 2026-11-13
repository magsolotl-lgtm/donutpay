package com.donutpay;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import net.fabricmc.loader.api.FabricLoader;

/** Stored at config/donutpay.json. Edit it, then run /donutpay reload. */
public class Config {
    public static final String DEFAULT_PATTERN =
            "(?<player>\\w{3,16}) (?:has )?(?:paid|sent) you \\$?(?<amount>[\\d,.]+ ?[KMBTkmbt]?)";

    public boolean enabled = true;
    public boolean sounds = true;
    /** Logs every system chat line to logs/latest.log so you can find the real payment format. */
    public boolean logSystemMessages = false;

    /** Needs two named groups: player and amount. */
    public String messagePattern = DEFAULT_PATTERN;

    public long small = 100_000L;
    public long medium = 500_000L;
    public long large = 1_000_000L;
    public long huge = 10_000_000L;
    public long universal = 50_000_000L;

    private transient Pattern compiled;

    public Pattern pattern() {
        if (compiled == null) {
            try {
                compiled = Pattern.compile(messagePattern);
                compiled.matcher("").groupCount();
            } catch (RuntimeException ex) {
                DonutPayClient.LOG.error("Invalid messagePattern, using the default", ex);
                compiled = Pattern.compile(DEFAULT_PATTERN);
            }
        }
        return compiled;
    }

    public static Config load() {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        Path file = FabricLoader.getInstance().getConfigDir().resolve("donutpay.json");
        try {
            if (Files.exists(file)) {
                Config c = gson.fromJson(Files.readString(file), Config.class);
                if (c != null) return c;
            }
            Config c = new Config();
            Files.writeString(file, gson.toJson(c));
            return c;
        } catch (IOException | RuntimeException ex) {
            DonutPayClient.LOG.error("Could not read config, using defaults", ex);
            return new Config();
        }
    }
}

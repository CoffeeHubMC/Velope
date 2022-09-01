package me.theseems.velope.config.user;

public class VelopePingerConfig {
    private Long cacheTtl;
    private Long pingInterval;
    private Long logUnavailableCooldown;

    public Long getCacheTtl() {
        return cacheTtl;
    }

    public Long getPingInterval() {
        return pingInterval;
    }

    public Long getLogUnavailableCooldown() {
        return logUnavailableCooldown;
    }
}

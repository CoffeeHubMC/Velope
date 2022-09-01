package me.theseems.velope.config.user;

public class VelopePingerConfig {
    private final Long cacheTtl;
    private final Long pingInterval;
    private final Long logUnavailableCooldown;

    public VelopePingerConfig(Long cacheTtl, Long pingInterval, Long logUnavailableCooldown) {
        this.cacheTtl = cacheTtl;
        this.pingInterval = pingInterval;
        this.logUnavailableCooldown = logUnavailableCooldown;
    }

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

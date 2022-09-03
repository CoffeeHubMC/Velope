package me.theseems.velope.config.user;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class VelopePingerConfig {
    private final Long cacheTtl;
    private final Long pingInterval;
    private final Long logUnavailableCooldown;
}

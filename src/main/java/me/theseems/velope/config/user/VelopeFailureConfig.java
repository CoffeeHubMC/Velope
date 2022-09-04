package me.theseems.velope.config.user;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class VelopeFailureConfig {
    private final Integer maxFailures;
    private final Long failureCleanInterval;

    public VelopeFailureConfig() {
        this.maxFailures = 2;
        this.failureCleanInterval = 45_000L;
    }
}

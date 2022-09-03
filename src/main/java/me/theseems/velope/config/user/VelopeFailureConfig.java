package me.theseems.velope.config.user;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class VelopeFailureConfig {
    private final Integer maxFailures;
    private final Long failureCleanInterval;

    public VelopeFailureConfig() {
        this.maxFailures = 10;
        this.failureCleanInterval = 10_000L;
    }
}

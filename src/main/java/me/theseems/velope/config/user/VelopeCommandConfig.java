package me.theseems.velope.config.user;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class VelopeCommandConfig {
    private final String label;
    private final List<String> aliases;
    private final String permission;
}

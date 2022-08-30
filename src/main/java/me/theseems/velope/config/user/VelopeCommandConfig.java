package me.theseems.velope.config.user;

import java.util.List;

public class VelopeCommandConfig {
    private final String label;
    private final List<String> aliases;
    private final String permission;

    public VelopeCommandConfig(String label, List<String> aliases, String permission) {
        this.label = label;
        this.aliases = aliases;
        this.permission = permission;
    }

    public String getLabel() {
        return label;
    }

    public List<String> getAliases() {
        return aliases;
    }

    public String getPermission() {
        return permission;
    }
}

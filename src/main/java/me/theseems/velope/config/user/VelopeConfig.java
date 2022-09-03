package me.theseems.velope.config.user;

import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
public class VelopeConfig {
    @SerializedName("integrations")
    private final VelopeIntegrationConfig integrationsConfig;
    private final List<VelopeGroupConfig> groups;
    @SerializedName("pingerSettings")
    private final VelopePingerConfig pingerConfig;
    private final String rootGroup;
    private final String initialGroup;
    private final Boolean redirectIfUnknownEnabled;
    private final Boolean fetchOnlineAlternativeEnabled;

    public VelopeConfig() {
        this.integrationsConfig = null;
        this.groups = new ArrayList<>();
        this.pingerConfig = new VelopePingerConfig(10_000L, 10_000L, 10_000L);
        this.rootGroup = null;
        this.initialGroup = null;
        this.redirectIfUnknownEnabled = true;
        this.fetchOnlineAlternativeEnabled = false;
    }
}

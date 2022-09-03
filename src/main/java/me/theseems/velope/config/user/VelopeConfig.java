package me.theseems.velope.config.user;

import com.google.gson.annotations.SerializedName;

import java.util.List;

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
        this.groups = null;
        this.pingerConfig = null;
        this.rootGroup = null;
        this.initialGroup = null;
        this.redirectIfUnknownEnabled = null;
        this.fetchOnlineAlternativeEnabled = null;
    }

    public VelopeConfig(VelopeIntegrationConfig integrationsConfig,
                        List<VelopeGroupConfig> groups,
                        VelopePingerConfig pingerConfig,
                        String rootGroup,
                        String initialGroup,
                        Boolean redirectIfUnknownEnabled,
                        Boolean fetchOnlineAlternativeEnabled) {
        this.integrationsConfig = integrationsConfig;
        this.groups = groups;
        this.pingerConfig = pingerConfig;
        this.rootGroup = rootGroup;
        this.initialGroup = initialGroup;
        this.redirectIfUnknownEnabled = redirectIfUnknownEnabled;
        this.fetchOnlineAlternativeEnabled = fetchOnlineAlternativeEnabled;
    }

    public VelopeIntegrationConfig getIntegrationsConfig() {
        return integrationsConfig;
    }

    public List<VelopeGroupConfig> getGroups() {
        return groups;
    }

    public VelopePingerConfig getPingerConfig() {
        return pingerConfig;
    }

    public String getRootGroup() {
        return rootGroup;
    }

    public String getInitialGroup() {
        return initialGroup;
    }

    public Boolean isRedirectIfUnknownEnabled() {
        return redirectIfUnknownEnabled;
    }

    public Boolean isFetchOnlineAlternativeEnabled() {
        return fetchOnlineAlternativeEnabled;
    }
}

package me.theseems.velope.config.user;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class VelopeConfig {
    private List<VelopeGroupConfig> groups;
    @SerializedName("pingerSettings")
    private VelopePingerConfig pingerConfig;
    private String rootGroup;
    private String initialGroup;
    private Boolean redirectIfUnknownEnabled;

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
}

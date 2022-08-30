package me.theseems.velope.config.user;

import java.util.List;

public class VelopeConfig {
    private List<VelopeGroupConfig> groups;
    private String rootGroup;
    private String initialGroup;

    public List<VelopeGroupConfig> getGroups() {
        return groups;
    }

    public String getRootGroup() {
        return rootGroup;
    }

    public String getInitialGroup() {
        return initialGroup;
    }
}

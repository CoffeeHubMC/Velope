package me.theseems.velope.config.user;

import me.theseems.velope.algo.BalanceStrategy;

import java.util.List;

public class VelopeGroupConfig {
    private final String name;
    private final List<String> servers;
    private final BalanceStrategy balanceStrategy;
    private final String parent;
    private final VelopeCommandConfig command;

    public VelopeGroupConfig(String name,
                             List<String> servers,
                             BalanceStrategy balanceStrategy,
                             String parent,
                             VelopeCommandConfig command) {
        this.name = name;
        this.servers = servers;
        this.balanceStrategy = balanceStrategy;
        this.parent = parent;
        this.command = command;
    }

    public String getName() {
        return name;
    }

    public List<String> getServers() {
        return servers;
    }

    public BalanceStrategy getBalanceStrategy() {
        return balanceStrategy;
    }

    public String getParent() {
        return parent;
    }

    public VelopeCommandConfig getCommand() {
        return command;
    }
}

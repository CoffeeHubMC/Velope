package me.theseems.velope.server;

import com.velocitypowered.api.proxy.server.RegisteredServer;
import me.theseems.velope.algo.BalanceStrategy;

import java.util.List;

public class VelopedServer {
    private final RegisteredServer registeredServer;
    private final String name;
    private final BalanceStrategy balanceStrategy;
    private VelopedServer parent;
    private final List<String> group;

    public VelopedServer(String name,
                         RegisteredServer registeredServer,
                         BalanceStrategy balanceStrategy,
                         VelopedServer parent,
                         List<String> group) {
        this.name = name;
        this.registeredServer = registeredServer;
        this.balanceStrategy = balanceStrategy;
        this.parent = parent;
        this.group = group;
    }

    public RegisteredServer getRegisteredServer() {
        return registeredServer;
    }

    public BalanceStrategy getBalanceStrategy() {
        return balanceStrategy;
    }

    public String getName() {
        return name;
    }

    public List<String> getGroup() {
        return group;
    }

    public VelopedServer getParent() {
        return parent;
    }

    public void setParent(VelopedServer parent) {
        this.parent = parent;
    }
}

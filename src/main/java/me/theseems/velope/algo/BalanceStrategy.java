package me.theseems.velope.algo;

import com.velocitypowered.api.proxy.server.ServerInfo;
import me.theseems.velope.algo.variety.FirstBalanceStrategy;
import me.theseems.velope.algo.variety.HighestBalanceStrategy;
import me.theseems.velope.algo.variety.LowestBalanceStrategy;
import me.theseems.velope.algo.variety.RandomBalanceStrategy;
import me.theseems.velope.server.VelopedServer;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public enum BalanceStrategy {
    FIRST(new FirstBalanceStrategy()),
    HIGHEST(new HighestBalanceStrategy()),
    LOWEST(new LowestBalanceStrategy()),
    RANDOM(new RandomBalanceStrategy());

    public interface BaseBalanceStrategy {
        Optional<ServerInfo> getOptimalServer(VelopedServer server, Collection<String> excluded, UUID playerUUID);
    }

    final BaseBalanceStrategy strategy;

    BalanceStrategy(BaseBalanceStrategy strategy) {
        this.strategy = strategy;
    }

    public BaseBalanceStrategy getStrategy() {
        return strategy;
    }
}

package me.theseems.velope.algo;

import com.google.inject.Inject;
import com.velocitypowered.api.proxy.server.ServerInfo;
import me.theseems.velope.Velope;
import me.theseems.velope.server.VelopedServer;
import me.theseems.velope.status.ServerStatus;
import me.theseems.velope.status.ServerStatusRepository;

import java.util.Optional;

public enum BalanceStrategy {
    FIRST(server -> server.getGroup().stream().findFirst()),
    HIGHEST(server -> {
        ServerInfo result = null;
        long maxAmount = -1;

        for (ServerInfo serverInfo : server.getGroup()) {
            Optional<ServerStatus> optionalServerStatus =
                    Velope.getStatusRepository().getStatus(serverInfo.getName());

            if (optionalServerStatus.isEmpty()) {
                continue;
            }

            ServerStatus status = optionalServerStatus.get();
            if (status.getPlayerCount() > maxAmount) {
                maxAmount = status.getPlayerCount();
                result = serverInfo;
            }
        }

        return Optional.ofNullable(result);
    });

    public interface BaseBalanceStrategy {
        Optional<ServerInfo> getOptimalServer(VelopedServer server);
    }

    final BaseBalanceStrategy strategy;

    BalanceStrategy(BaseBalanceStrategy strategy) {
        this.strategy = strategy;
    }

    public BaseBalanceStrategy getStrategy() {
        return strategy;
    }
}

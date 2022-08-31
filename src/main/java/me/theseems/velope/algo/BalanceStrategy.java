package me.theseems.velope.algo;

import com.velocitypowered.api.proxy.server.ServerInfo;
import me.theseems.velope.Velope;
import me.theseems.velope.server.VelopedServer;
import me.theseems.velope.status.ServerStatus;

import java.util.Optional;

public enum BalanceStrategy {
    FIRST(server -> server.getGroup().stream().findFirst()),
    HIGHEST(server -> {
        if (server == null || server.getGroup().isEmpty()) {
            return Optional.empty();
        }

        ServerInfo result = server.getGroup().get(0);
        long maxAmount = Velope.getStatusRepository()
                .getStatus(result.getName())
                .map(ServerStatus::getPlayerCount)
                .orElse(-1L);

        for (ServerInfo serverInfo : server.getGroup()) {
            Optional<ServerStatus> optionalServerStatus =
                    Velope.getStatusRepository().getStatus(serverInfo.getName());

            if (optionalServerStatus.isEmpty()) {
                continue;
            }

            ServerStatus status = optionalServerStatus.get();
            if (status.getPlayerCount() > maxAmount && status.getPlayerCount() + 1 <= status.getMaxPlayerCount()) {
                maxAmount = status.getPlayerCount();
                result = serverInfo;
            }
        }

        return Optional.of(result);
    }),
    LOWEST(server -> {
        if (server == null || server.getGroup().isEmpty()) {
            return Optional.empty();
        }

        ServerInfo result = server.getGroup().get(0);
        long minAmount = Velope.getStatusRepository()
                .getStatus(result.getName())
                .map(ServerStatus::getPlayerCount)
                .orElse(Long.MAX_VALUE);

        for (ServerInfo serverInfo : server.getGroup()) {
            Optional<ServerStatus> optionalServerStatus =
                    Velope.getStatusRepository().getStatus(serverInfo.getName());

            if (optionalServerStatus.isEmpty()) {
                continue;
            }

            ServerStatus status = optionalServerStatus.get();
            if (status.getPlayerCount() < minAmount && status.getPlayerCount() + 1 <= status.getMaxPlayerCount()) {
                minAmount = status.getPlayerCount();
                result = serverInfo;
            }
        }

        return Optional.of(result);
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

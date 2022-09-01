package me.theseems.velope.algo;

import com.velocitypowered.api.proxy.server.ServerInfo;
import me.theseems.velope.Velope;
import me.theseems.velope.server.VelopedServer;
import me.theseems.velope.status.ServerStatus;

import java.util.Optional;

public enum BalanceStrategy {
    FIRST(server -> server.getGroup().stream()
            .findFirst()
            .flatMap(first -> Velope.getStatusRepository().getStatus(first))
            .map(ServerStatus::getServerInfo)),
    HIGHEST(server -> {
        if (server == null || server.getGroup().isEmpty()) {
            return Optional.empty();
        }

        ServerStatus result = Velope.getStatusRepository()
                .getStatus(server.getGroup().get(0))
                .orElse(null);

        long maxAmount = Optional.ofNullable(result)
                .map(ServerStatus::getPlayerCount)
                .orElse(-1L);

        for (String child : server.getGroup()) {
            Optional<ServerStatus> optionalServerStatus =
                    Velope.getStatusRepository().getStatus(child);

            if (optionalServerStatus.isEmpty()) {
                continue;
            }

            ServerStatus status = optionalServerStatus.get();
            if (status.isAvailable()
                    && status.getPlayerCount() > maxAmount
                    && status.getPlayerCount() + 1 <= status.getMaxPlayerCount()) {
                maxAmount = status.getPlayerCount();
                result = status;
            }
        }

        return Optional.ofNullable(result)
                .map(ServerStatus::getServerInfo);
    }),
    LOWEST(server -> {
        if (server == null || server.getGroup().isEmpty()) {
            return Optional.empty();
        }

        ServerStatus result = Velope.getStatusRepository()
                .getStatus(server.getGroup().get(0))
                .orElse(null);
        long minAmount = Optional.ofNullable(result)
                .map(ServerStatus::getPlayerCount)
                .orElse(Long.MAX_VALUE);

        for (String child : server.getGroup()) {
            Optional<ServerStatus> optionalServerStatus =
                    Velope.getStatusRepository().getStatus(child);

            if (optionalServerStatus.isEmpty()) {
                continue;
            }

            ServerStatus status = optionalServerStatus.get();
            if (status.isAvailable()
                    && status.getPlayerCount() < minAmount
                    && status.getPlayerCount() + 1 <= status.getMaxPlayerCount()) {
                minAmount = status.getPlayerCount();
                result = status;
            }
        }

        return Optional.ofNullable(result)
                .map(ServerStatus::getServerInfo);
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

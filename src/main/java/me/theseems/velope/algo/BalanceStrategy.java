package me.theseems.velope.algo;

import com.velocitypowered.api.proxy.server.ServerInfo;
import me.theseems.velope.Velope;
import me.theseems.velope.server.VelopedServer;
import me.theseems.velope.status.ServerStatus;

import java.util.*;
import java.util.stream.Collectors;

public enum BalanceStrategy {

    FIRST((server, excluded) -> {
        Optional<ServerInfo> result = server.getGroup().stream()
                .findFirst()
                .flatMap(first -> Velope.getStatusRepository().getStatus(first))
                .map(ServerStatus::getServerInfo);
        if (result.isPresent() && excluded.contains(result.get().getName())) {
            return Optional.empty();
        }
        return result;
    }),
    HIGHEST((server, excluded) -> {
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
            if (excluded.contains(child)) {
                continue;
            }

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

        if (result != null && excluded.contains(result.getServerInfo().getName())) {
            return Optional.empty();
        }

        return Optional.ofNullable(result)
                .map(ServerStatus::getServerInfo);
    }),
    LOWEST((server, excluded) -> {
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
            if (excluded.contains(child)) {
                continue;
            }

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

        if (result != null && excluded.contains(result.getServerInfo().getName())) {
            return Optional.empty();
        }

        return Optional.ofNullable(result)
                .map(ServerStatus::getServerInfo);
    }),
    RANDOM((server, excluded) -> {
        if (server == null || server.getGroup().isEmpty()) {
            return Optional.empty();
        }

        List<ServerInfo> candidates = server.getGroup().stream()
                .map(example -> Velope.getStatusRepository().getStatus(example))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .filter(example -> example.isAvailable() && example.getPlayerCount() + 1 < example.getMaxPlayerCount())
                .map(ServerStatus::getServerInfo)
                .filter(serverInfo -> !excluded.contains(serverInfo.getName()))
                .collect(Collectors.toList());

        if (candidates.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(candidates.get(new Random().nextInt(candidates.size())));
    });

    public interface BaseBalanceStrategy {
        Optional<ServerInfo> getOptimalServer(VelopedServer server, Collection<String> excluded);
    }

    final BaseBalanceStrategy strategy;

    BalanceStrategy(BaseBalanceStrategy strategy) {
        this.strategy = strategy;
    }

    public BaseBalanceStrategy getStrategy() {
        return strategy;
    }
}

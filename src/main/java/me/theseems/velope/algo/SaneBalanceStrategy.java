package me.theseems.velope.algo;

import com.velocitypowered.api.proxy.server.ServerInfo;
import me.theseems.velope.Velope;
import me.theseems.velope.server.VelopedServer;
import me.theseems.velope.status.ServerStatus;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

public abstract class SaneBalanceStrategy implements BalanceStrategy.BaseBalanceStrategy {
    @Override
    public Optional<ServerInfo> getOptimalServer(VelopedServer server, Collection<String> excluded, UUID playerUUID) {
        if (server == null || server.getGroup().isEmpty()) {
            return Optional.empty();
        }

        long maxFailures = Velope
                .getVelopeConfig()
                .getVelopeFailureConfig()
                .getMaxFailures();

        List<ServerStatus> saneServers = server.getGroup()
                .stream()
                .map(name -> Velope.getStatusRepository().getStatus(name))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .filter(status -> status.isAvailable() && status.getPlayerCount() + 1 <= status.getMaxPlayerCount())
                .filter(status -> {
                    long currentFailures = Velope.getHistoryRepository()
                            .getFailures(playerUUID, status.getServerInfo().getName());
                    return currentFailures <= maxFailures;
                }).collect(Collectors.toList());

        if (saneServers.isEmpty()) {
            return Optional.empty();
        }

        return getSaneOptimalServer(server, saneServers, playerUUID);
    }

    public abstract Optional<ServerInfo> getSaneOptimalServer(VelopedServer server,
                                                              Collection<ServerStatus> saneServers,
                                                              UUID playerUUID);
}

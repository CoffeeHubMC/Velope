package me.theseems.velope.algo;

import com.velocitypowered.api.proxy.server.ServerInfo;
import me.theseems.velope.Velope;
import me.theseems.velope.history.RedirectHistoryRepository;
import me.theseems.velope.server.VelopedServer;
import me.theseems.velope.status.ServerStatus;

import java.util.*;
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

        RedirectHistoryRepository repository = Velope.getHistoryRepository();
        Map<Long, Set<ServerStatus>> grouped = server.getGroup()
                .stream()
                .map(name -> Velope.getStatusRepository().getStatus(name))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .filter(status -> status.isAvailable() && status.getPlayerCount() + 1 <= status.getMaxPlayerCount())
                .filter(status -> repository.getFailures(playerUUID, status.getServerInfo().getName()) <= maxFailures)
                // Grouping servers base on failure attempts
                // First of all, we'll try to get server with 0 failures
                // Then, with 1 failure and on and on.
                .collect(Collectors.groupingBy(
                        status -> repository.getFailures(playerUUID, status.getServerInfo().getName()),
                        Collectors.toSet()));

        for (Long failureCount : grouped.keySet()) {
            Optional<ServerInfo> serverInfo = getSaneOptimalServer(server, grouped.get(failureCount), playerUUID);
            if (serverInfo.isPresent()) {
                return serverInfo;
            }
        }

        return Optional.empty();
    }

    public abstract Optional<ServerInfo> getSaneOptimalServer(VelopedServer server,
                                                              Collection<ServerStatus> saneServers,
                                                              UUID playerUUID);
}

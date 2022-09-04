package me.theseems.velope.algo.variety;

import com.velocitypowered.api.proxy.server.ServerInfo;
import me.theseems.velope.algo.SaneBalanceStrategy;
import me.theseems.velope.server.VelopedServer;
import me.theseems.velope.status.ServerStatus;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public class LowestBalanceStrategy extends SaneBalanceStrategy {
    @Override
    public Optional<ServerInfo> getSaneOptimalServer(VelopedServer server, Collection<ServerStatus> saneServers, UUID playerUUID) {
        ServerStatus result = saneServers.iterator().next();
        long maxAmount = result.getPlayerCount();

        for (ServerStatus status : saneServers) {
            if (status.getPlayerCount() < maxAmount) {
                maxAmount = status.getPlayerCount();
                result = status;
            }
        }

        return Optional.of(result).map(ServerStatus::getServerInfo);
    }
}

package me.theseems.velope.algo.variety;

import com.velocitypowered.api.proxy.server.ServerInfo;
import me.theseems.velope.algo.SaneBalanceStrategy;
import me.theseems.velope.server.VelopedServer;
import me.theseems.velope.status.ServerStatus;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public class FirstBalanceStrategy extends SaneBalanceStrategy {
    @Override
    public Optional<ServerInfo> getSaneOptimalServer(VelopedServer server, Collection<ServerStatus> saneServers, UUID playerUUID) {
        return saneServers.isEmpty()
                ? Optional.empty()
                : Optional.ofNullable(saneServers.iterator().next().getServerInfo());
    }
}

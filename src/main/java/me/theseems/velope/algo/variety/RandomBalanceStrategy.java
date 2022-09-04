package me.theseems.velope.algo.variety;

import com.velocitypowered.api.proxy.server.ServerInfo;
import me.theseems.velope.algo.SaneBalanceStrategy;
import me.theseems.velope.server.VelopedServer;
import me.theseems.velope.status.ServerStatus;

import java.util.*;

public class RandomBalanceStrategy extends SaneBalanceStrategy {
    @Override
    public Optional<ServerInfo> getSaneOptimalServer(VelopedServer server, Collection<ServerStatus> saneServers, UUID playerUUID) {
        return Optional.of(new ArrayList<>(saneServers).get(new Random().nextInt(saneServers.size())))
                .map(ServerStatus::getServerInfo);
    }
}

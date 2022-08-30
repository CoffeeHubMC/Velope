package me.theseems.velope.status;

import com.velocitypowered.api.proxy.server.ServerInfo;
import net.kyori.adventure.text.Component;

public class ServerStatus {
    private final ServerInfo serverInfo;
    private final Component motd;
    private final boolean available;
    private final long playerCount;
    private final long maxPlayerCount;

    public ServerStatus(ServerInfo serverInfo,
                        Component motd,
                        boolean available,
                        long playerCount,
                        long maxPlayerCount) {
        this.serverInfo = serverInfo;
        this.motd = motd;
        this.available = available;
        this.playerCount = playerCount;
        this.maxPlayerCount = maxPlayerCount;
    }

    public ServerInfo getServerInfo() {
        return serverInfo;
    }

    public Component getMotd() {
        return motd;
    }

    public long getPlayerCount() {
        return playerCount;
    }

    public long getMaxPlayerCount() {
        return maxPlayerCount;
    }

    public boolean isAvailable() {
        return available;
    }

    @Override
    public String toString() {
        return "ServerStatus{" +
                "serverInfo=" + serverInfo +
                ", motd=" + motd +
                ", playerCount=" + playerCount +
                ", maxPlayerCount=" + maxPlayerCount +
                '}';
    }
}

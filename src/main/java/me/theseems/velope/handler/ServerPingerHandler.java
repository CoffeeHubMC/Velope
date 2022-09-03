package me.theseems.velope.handler;

import com.google.inject.Inject;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerPing;
import me.theseems.velope.Velope;
import me.theseems.velope.config.user.VelopeConfig;
import me.theseems.velope.server.VelopedServer;
import me.theseems.velope.server.VelopedServerRepository;
import me.theseems.velope.status.ServerStatus;
import me.theseems.velope.status.ServerStatusRepository;

import javax.inject.Named;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class ServerPingerHandler implements Runnable {
    @Inject
    private ServerStatusRepository statusRepository;
    @Inject
    private VelopedServerRepository velopedServerRepository;
    @Inject
    private Velope velope;
    @Inject
    private VelopeConfig velopeConfig;

    @Inject
    @Named("cacheTtl")
    private Duration cacheTtl;
    @Inject
    @Named("logUnavailableCooldown")
    private Duration logUnavailableCooldown;

    static class CacheEntry {
        private ZonedDateTime latestRetreive;
        private ZonedDateTime lastLogUnavailable;

        public CacheEntry(ZonedDateTime latestRetreive) {
            this.latestRetreive = latestRetreive;
        }
    }

    private final Map<String, CacheEntry> cacheEntryMap;

    public ServerPingerHandler() {
        cacheEntryMap = new ConcurrentHashMap<>();
    }

    private void fetch(String serverName, CacheEntry cacheEntry) {
        Optional<RegisteredServer> registeredServerOptional =
                velope.getProxyServer().getServer(serverName);
        if (registeredServerOptional.isEmpty()) {
            statusRepository.deleteStatus(serverName);
            return;
        }

        RegisteredServer registeredServer = registeredServerOptional.get();
        registeredServer.ping().whenCompleteAsync((serverPing, throwable) -> {
            boolean available = true;
            if (throwable != null) {
                available = false;
                logUnavailable(cacheEntry,
                        "Could not ping server '" + serverName + "'");
            }
            if (available && serverPing.getPlayers().isEmpty()) {
                available = false;
                logUnavailable(cacheEntry,
                        "Could not find players on server '" + serverName + "'");
            }

            boolean onlineAlternative = Optional
                    .ofNullable(velopeConfig.getFetchOnlineAlternativeEnabled())
                    .orElse(false);

            ServerStatus status = new ServerStatus(
                    registeredServer.getServerInfo(),
                    serverPing.getDescriptionComponent(),
                    available,
                    onlineAlternative
                            ? registeredServer.getPlayersConnected().size()
                            : serverPing.getPlayers().map(ServerPing.Players::getOnline).orElse(0),
                    serverPing.getPlayers().map(ServerPing.Players::getMax).orElse(0));

            statusRepository.saveStatus(status);
        });
    }

    @Override
    public void run() {
        for (VelopedServer velopedServer : velopedServerRepository.findAll()) {
            for (String child : velopedServer.getGroup()) {
                CacheEntry entry = cacheEntryMap.get(child);
                if (entry == null
                        || Duration.between(entry.latestRetreive, ZonedDateTime.now()).compareTo(cacheTtl) > 0) {
                    fetch(child, entry);
                    if (!cacheEntryMap.containsKey(child)) {
                        cacheEntryMap.put(child, new CacheEntry(ZonedDateTime.now()));
                    } else {
                        cacheEntryMap.get(child).latestRetreive = ZonedDateTime.now();
                    }
                }
            }
        }

        // Wipe out expired
        cacheEntryMap.forEach((serverInfo, cacheEntry) -> {
            if (Duration.between(cacheEntry.latestRetreive, ZonedDateTime.now()).compareTo(cacheTtl) > 0) {
                cacheEntryMap.remove(serverInfo);
            }
        });
    }

    private void logUnavailable(CacheEntry entry, String message) {
        if (entry.lastLogUnavailable == null
                || Duration.between(entry.lastLogUnavailable, ZonedDateTime.now()).compareTo(logUnavailableCooldown) > 0) {
            velope.getLogger().warn(message);
            entry.lastLogUnavailable = ZonedDateTime.now();
        }
    }
}

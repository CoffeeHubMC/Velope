package me.theseems.velope.handler;

import com.google.inject.Inject;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerInfo;
import com.velocitypowered.api.proxy.server.ServerPing;
import me.theseems.velope.Velope;
import me.theseems.velope.server.VelopedServer;
import me.theseems.velope.server.VelopedServerRepository;
import me.theseems.velope.status.ServerStatus;
import me.theseems.velope.status.ServerStatusRepository;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class ServerPingerHandler implements Runnable {
    private static final Duration CACHE_TTL = Duration.ofSeconds(10);
    private static final Duration LOG_UNAVAILABLE_COOLDOWN = Duration.ofMinutes(1);

    @Inject
    private ServerStatusRepository statusRepository;
    @Inject
    private VelopedServerRepository velopedServerRepository;
    @Inject
    private Velope velope;

    static class CacheEntry {
        private ZonedDateTime latestRetreive;
        private ZonedDateTime lastLogUnavailable;

        public CacheEntry(ZonedDateTime latestRetreive) {
            this.latestRetreive = latestRetreive;
        }
    }

    private final Map<ServerInfo, CacheEntry> cacheEntryMap;

    public ServerPingerHandler() {
        cacheEntryMap = new ConcurrentHashMap<>();
    }

    private void fetch(ServerInfo serverInfo, CacheEntry cacheEntry) {
        Optional<RegisteredServer> registeredServerOptional =
                velope.getProxyServer().getServer(serverInfo.getName());
        if (registeredServerOptional.isEmpty()) {
            statusRepository.deleteStatus(serverInfo.getName());
            return;
        }

        RegisteredServer registeredServer = registeredServerOptional.get();
        registeredServer.ping().whenCompleteAsync((serverPing, throwable) -> {
            boolean available = true;
            if (throwable != null) {
                available = false;

                if (cacheEntry.lastLogUnavailable == null
                        || Duration.between(cacheEntry.lastLogUnavailable, ZonedDateTime.now()).compareTo(LOG_UNAVAILABLE_COOLDOWN) > 0) {
                    velope.getLogger().warn("Could not ping server '" + serverInfo.getName() + "'");
                    cacheEntry.lastLogUnavailable = ZonedDateTime.now();
                }
            }
            if (available && serverPing.getPlayers().isEmpty()) {
                available = false;

                if (cacheEntry.lastLogUnavailable == null
                        || Duration.between(cacheEntry.lastLogUnavailable, ZonedDateTime.now()).compareTo(LOG_UNAVAILABLE_COOLDOWN) > 0) {
                    velope.getLogger().warn("Could not find players on server '" + serverInfo.getName() + "'");
                    cacheEntry.lastLogUnavailable = ZonedDateTime.now();
                }
            }

            ServerStatus status = new ServerStatus(
                    serverInfo,
                    serverPing.getDescriptionComponent(),
                    available,
                    serverPing.getPlayers().map(ServerPing.Players::getOnline).orElse(0),
                    serverPing.getPlayers().map(ServerPing.Players::getMax).orElse(0));

            statusRepository.saveStatus(status);
        });
    }

    @Override
    public void run() {
        for (VelopedServer velopedServer : velopedServerRepository.findAll()) {
            for (ServerInfo serverInfo : velopedServer.getGroup()) {
                CacheEntry entry = cacheEntryMap.get(serverInfo);
                if (entry == null
                        || Duration.between(entry.latestRetreive, ZonedDateTime.now()).compareTo(CACHE_TTL) > 0) {
                    fetch(serverInfo, entry);
                    if (!cacheEntryMap.containsKey(serverInfo)) {
                        cacheEntryMap.put(serverInfo, new CacheEntry(ZonedDateTime.now()));
                    } else {
                        cacheEntryMap.get(serverInfo).latestRetreive = ZonedDateTime.now();
                    }
                }
            }
        }

        // Wipe out expired
        cacheEntryMap.forEach((serverInfo, cacheEntry) -> {
            if (Duration.between(cacheEntry.latestRetreive, ZonedDateTime.now()).compareTo(CACHE_TTL) > 0) {
                cacheEntryMap.remove(serverInfo);
            }
        });
    }
}

package me.theseems.velope.history;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.Consumer;

public class MemoryRedirectHistoryRepository implements RedirectHistoryRepository {
    private final Map<UUID, RedirectEntry> redirectEntryMap;
    private final Map<UUID, Collection<Consumer<RedirectEntry>>> oneTimeSubscribersMap;

    private final Map<UUID, Map<String, Long>> failureMap;

    public MemoryRedirectHistoryRepository() {
        redirectEntryMap = new ConcurrentHashMap<>();
        oneTimeSubscribersMap = new ConcurrentHashMap<>();
        failureMap = new ConcurrentHashMap<>();
    }

    @Override
    public void setLatestRedirect(RedirectEntry entry) {
        redirectEntryMap.put(entry.getPlayerUUID(), entry);
        if (oneTimeSubscribersMap.containsKey(entry.getPlayerUUID())) {
            for (Consumer<RedirectEntry> consumer : oneTimeSubscribersMap.get(entry.getPlayerUUID())) {
                consumer.accept(entry);
            }
            oneTimeSubscribersMap.remove(entry.getPlayerUUID());
        }
    }

    @Override
    public void removeLastRedirect(UUID playerUUID) {
        redirectEntryMap.remove(playerUUID);
    }

    @Override
    public long getFailures(UUID playerUUID, String server) {
        return Optional.ofNullable(failureMap.get(playerUUID))
                .flatMap(map -> Optional.ofNullable(map.get(server)))
                .orElse(0L);
    }

    @Override
    public void addFailure(UUID playerUUID, String server) {
        failureMap.putIfAbsent(playerUUID, new ConcurrentHashMap<>());
        Map<String, Long> countMap = failureMap.get(playerUUID);
        countMap.putIfAbsent(server, 0L);
        countMap.put(server, countMap.get(server) + 1);
    }

    @Override
    public void cleanFailures() {
        failureMap.clear();
    }

    @Override
    public void cleanLatestRedirects() {
        redirectEntryMap.clear();
    }

    @Override
    public Map<String, Long> getFailureMap(UUID playerUUID) {
        return failureMap.get(playerUUID);
    }

    @Override
    public Optional<RedirectEntry> getLatestRedirect(UUID playerUUID) {
        return Optional.ofNullable(redirectEntryMap.get(playerUUID));
    }

    @Override
    public void subscribeOnce(UUID playerUUID, Consumer<RedirectEntry> consumer) {
        if (!oneTimeSubscribersMap.containsKey(playerUUID)) {
            oneTimeSubscribersMap.put(playerUUID, new CopyOnWriteArraySet<>());
        }
        oneTimeSubscribersMap.get(playerUUID).add(consumer);
    }
}

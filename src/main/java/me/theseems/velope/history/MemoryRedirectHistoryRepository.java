package me.theseems.velope.history;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.Consumer;

public class MemoryRedirectHistoryRepository implements RedirectHistoryRepository {
    private final Map<UUID, RedirectEntry> redirectEntryMap;
    private final Map<UUID, Collection<Consumer<RedirectEntry>>> oneTimeSubscribersMap;

    public MemoryRedirectHistoryRepository() {
        redirectEntryMap = new ConcurrentHashMap<>();
        oneTimeSubscribersMap = new ConcurrentHashMap<>();
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
    public void remove(UUID playerUUID) {
        redirectEntryMap.remove(playerUUID);
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

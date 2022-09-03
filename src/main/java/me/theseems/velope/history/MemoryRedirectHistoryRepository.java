package me.theseems.velope.history;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class MemoryRedirectHistoryRepository implements RedirectHistoryRepository {
    private Map<UUID, RedirectEntry> redirectEntryMap;

    public MemoryRedirectHistoryRepository() {
        redirectEntryMap = new ConcurrentHashMap<>();
    }

    @Override
    public void setLatestRedirect(RedirectEntry entry) {
        redirectEntryMap.put(entry.getPlayerUUID(), entry);
    }

    @Override
    public Optional<RedirectEntry> getLatestRedirect(UUID playerUUID) {
        return Optional.ofNullable(redirectEntryMap.get(playerUUID));
    }
}

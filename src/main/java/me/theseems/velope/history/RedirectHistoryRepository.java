package me.theseems.velope.history;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

public interface RedirectHistoryRepository {
    void setLatestRedirect(RedirectEntry entry);
    void removeLastRedirect(UUID playerUUID);
    Optional<RedirectEntry> getLatestRedirect(UUID playerUUID);
    void cleanLatestRedirects();
    void subscribeOnce(UUID playerUUID, Consumer<RedirectEntry> consumer);

    long getFailures(UUID playerUUID, String server);
    void addFailure(UUID playerUUID, String server);
    Map<String, Long> getFailureMap(UUID playerUUID);
    void cleanFailures();
}

package me.theseems.velope.history;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

public interface RedirectHistoryRepository {
    void setLatestRedirect(RedirectEntry entry);
    void remove(UUID playerUUID);
    Optional<RedirectEntry> getLatestRedirect(UUID playerUUID);

    void subscribeOnce(UUID playerUUID, Consumer<RedirectEntry> consumer);
}

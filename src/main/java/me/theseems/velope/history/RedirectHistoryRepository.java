package me.theseems.velope.history;

import java.util.Optional;
import java.util.UUID;

public interface RedirectHistoryRepository {
    void setLatestRedirect(RedirectEntry entry);
    Optional<RedirectEntry> getLatestRedirect(UUID playerUUID);
}

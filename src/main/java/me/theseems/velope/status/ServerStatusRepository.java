package me.theseems.velope.status;

import java.util.Optional;

public interface ServerStatusRepository {
    Optional<ServerStatus> getStatus(String name);
    void saveStatus(ServerStatus status);
    void deleteStatus(String name);
    void deleteAll();
}

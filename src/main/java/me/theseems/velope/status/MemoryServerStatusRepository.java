package me.theseems.velope.status;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class MemoryServerStatusRepository implements ServerStatusRepository {
    private final Map<String, ServerStatus> serverStatusMap;

    public MemoryServerStatusRepository() {
        serverStatusMap = new ConcurrentHashMap<>();
    }

    @Override
    public Optional<ServerStatus> getStatus(String name) {
        return Optional.ofNullable(serverStatusMap.get(name));
    }

    @Override
    public void saveStatus(ServerStatus status) {
        serverStatusMap.put(status.getServerInfo().getName(), status);
    }

    @Override
    public void deleteStatus(String name) {
        serverStatusMap.remove(name);
    }

    @Override
    public void deleteAll() {
        serverStatusMap.clear();
    }
}

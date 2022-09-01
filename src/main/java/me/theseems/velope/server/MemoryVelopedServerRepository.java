package me.theseems.velope.server;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MemoryVelopedServerRepository implements VelopedServerRepository {
    private final Map<String, VelopedServer> velopedServerMap;
    private final Map<String, VelopedServer> commonServerMap;

    public MemoryVelopedServerRepository() {
        velopedServerMap = new ConcurrentHashMap<>();
        commonServerMap = new ConcurrentHashMap<>();
    }

    @Override
    public VelopedServer getServer(String name) {
        return velopedServerMap.get(name);
    }

    @Override
    public void addServer(VelopedServer velopedServer) {
        velopedServerMap.put(velopedServer.getName(), velopedServer);
        for (String serverName : velopedServer.getGroup()) {
            if (commonServerMap.containsKey(serverName)) {
                throw new IllegalArgumentException("Multiple parent for server '" + serverName + "'");
            }

            commonServerMap.put(serverName, velopedServer);
        }
    }

    @Override
    public void deleteServer(String name) {
        VelopedServer toBeDeleted = velopedServerMap.get(name);
        toBeDeleted.getGroup().forEach(commonServerMap::remove);

        velopedServerMap.remove(name);
    }

    @Override
    public VelopedServer getParent(String server) {
        if (commonServerMap.containsKey(server)) {
            return commonServerMap.get(server);
        }

        return null;
    }

    @Override
    public Collection<VelopedServer> findAll() {
        return velopedServerMap.values();
    }

    @Override
    public void deleteAll() {
        velopedServerMap.clear();
        commonServerMap.clear();
    }
}

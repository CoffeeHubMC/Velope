package me.theseems.velope.server;

import java.util.Collection;
import java.util.Optional;

public interface VelopedServerRepository {
    VelopedServer getServer(String name);
    void addServer(VelopedServer velopedServer);
    void deleteServer(String name);
    VelopedServer getParent(String server);
    Collection<VelopedServer> findAll();
    void deleteAll();
    default Optional<VelopedServer> findParent(String server) {
        return Optional.ofNullable(getParent(server));
    }
}

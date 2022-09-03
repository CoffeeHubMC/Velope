package me.theseems.velope.utils;

import com.velocitypowered.api.proxy.ConnectionRequestBuilder;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import me.theseems.velope.Velope;
import me.theseems.velope.history.RedirectEntry;
import me.theseems.velope.server.VelopedServer;
import me.theseems.velope.server.VelopedServerRepository;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

public class ConnectionUtils {
    public static void connectAndSupervise(ProxyServer proxyServer, Player player, VelopedServer velopedServer) {
        velopedServer.getBalanceStrategy()
                .getStrategy()
                .getOptimalServer(velopedServer, getExclusionListForPlayer(player), player.getUniqueId())
                .flatMap(serverInfo -> proxyServer.getServer(serverInfo.getName()))
                .ifPresentOrElse(
                        (server) -> connectAndSupervise(player, server, velopedServer),
                        () -> player.sendMessage(
                                Component.text("Sorry. Could not find destination. Try again later.")
                                        .color(NamedTextColor.RED)
                        )
                );
    }

    public static void connectAndSupervise(Player player, RegisteredServer registeredServer) {
        connectAndSupervise(player, registeredServer, null);
    }

    public static void connectAndSupervise(Player player, RegisteredServer registeredServer, VelopedServer from) {
        if (registeredServer == null) {
            return;
        }

        Velope.getHistoryRepository().setLatestRedirect(new RedirectEntry(
                player.getUniqueId(),
                from,
                registeredServer.getServerInfo().getName()
        ));

        player.createConnectionRequest(registeredServer)
                .connect()
                .whenCompleteAsync((result, throwable) -> {
                    if (!result.isSuccessful() || throwable != null) {
                        if (result.getStatus() != ConnectionRequestBuilder.Status.ALREADY_CONNECTED
                        && result.getStatus() != ConnectionRequestBuilder.Status.CONNECTION_IN_PROGRESS) {
                            Velope.getHistoryRepository().addFailure(
                                    player.getUniqueId(),
                                    registeredServer.getServerInfo().getName());
                        }

                        player.sendMessage(
                                Component.text("Cannot connect you: ")
                                        .color(NamedTextColor.RED)
                                        .append(throwable != null
                                                ? Component.text(throwable.getMessage())
                                                : result.getReasonComponent().orElse(
                                                Component.text(result.getStatus().name()))));
                    }
                });
    }

    public static RegisteredServer findNearestAvailable(VelopedServerRepository repository,
                                                        ProxyServer proxyServer,
                                                        UUID playerUUID,
                                                        String serverName,
                                                        Collection<String> excluded) {
        if (serverName == null) {
            return null;
        }

        VelopedServer parent = repository.getParent(serverName);
        return findNearestAvailable(
                proxyServer,
                playerUUID,
                parent == null
                        ? repository.getServer(serverName)
                        : parent,
                excluded);
    }

    public static RegisteredServer findNearestAvailable(ProxyServer proxyServer,
                                                        UUID playerUUID,
                                                        VelopedServer origin,
                                                        Collection<String> excluded) {
        if (origin == null) {
            return null;
        }

        RegisteredServer registeredServer = null;
        while (origin != null) {
            Optional<RegisteredServer> server = origin.getBalanceStrategy()
                    .getStrategy()
                    .getOptimalServer(origin, excluded, playerUUID)
                    .flatMap(serverInfo -> proxyServer.getServer(serverInfo.getName()));

            if (server.isPresent()) {
                registeredServer = server.get();
                break;
            }

            origin = origin.getParent();
        }

        return registeredServer;
    }

    public static Collection<String> getExclusionListForPlayer(Player player) {
        return player.getCurrentServer()
                .map(serverConnection -> Collections.singleton(serverConnection.getServerInfo().getName()))
                .orElse(Collections.emptySet());
    }

    public static RegisteredServer findWithBalancer(ProxyServer proxyServer,
                                                    VelopedServer velopedServer,
                                                    UUID playerUUID,
                                                    Collection<String> excluded) {
        return velopedServer
                .getBalanceStrategy()
                .getStrategy()
                .getOptimalServer(velopedServer, excluded, playerUUID)
                .flatMap(serverInfo -> proxyServer.getServer(serverInfo.getName()))
                .orElse(null);
    }
}

package me.theseems.velope.listener;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.ServerPreConnectEvent;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerInfo;
import me.theseems.velope.Velope;
import me.theseems.velope.server.VelopedServer;
import me.theseems.velope.server.VelopedServerRepository;
import net.kyori.adventure.text.Component;

import java.util.Optional;

import static me.theseems.velope.utils.ConnectionUtils.findNearestAvailable;

public class VelopeServerJoinListener {
    @Inject
    private VelopedServerRepository repository;
    @Inject
    private Velope velope;

    @Subscribe
    public void onPlayerJoin(ServerPreConnectEvent event) {
        Optional<VelopedServer> velopedServer = Optional.ofNullable(event.getOriginalServer())
                .map(RegisteredServer::getServerInfo)
                .map(ServerInfo::getName)
                .map(info -> repository.getServer(info));

        if (velopedServer.isEmpty()) {
            return;
        }

        RegisteredServer registeredServer = findNearestAvailable(
                repository,
                velope.getProxyServer(),
                event.getOriginalServer().getServerInfo().getName());

        if (registeredServer == null) {
            event.setResult(ServerPreConnectEvent.ServerResult.denied());
            event.getPlayer().disconnect(Component.text("Sorry." +
                    " We have a problem finding the server for you. Please, try again later."));
            velope.getLogger().warn(String.format("No server found for player '%s' @ %s from %s",
                    event.getPlayer().getUsername(),
                    velopedServer.get().getName(),
                    event.getOriginalServer().getServerInfo().getName()));
            return;
        }

        event.setResult(ServerPreConnectEvent.ServerResult.allowed(registeredServer));
    }
}

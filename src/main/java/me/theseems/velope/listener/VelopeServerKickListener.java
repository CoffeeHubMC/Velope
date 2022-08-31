package me.theseems.velope.listener;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.KickedFromServerEvent;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import me.theseems.velope.Velope;
import me.theseems.velope.config.user.VelopeConfig;
import me.theseems.velope.server.VelopedServer;
import me.theseems.velope.server.VelopedServerRepository;
import net.kyori.adventure.text.Component;

import java.util.Optional;

import static me.theseems.velope.utils.ConnectionUtils.findNearestAvailable;
import static me.theseems.velope.utils.ConnectionUtils.findWithBalancer;

public class VelopeServerKickListener {
    @Inject
    private VelopedServerRepository velopedServerRepository;
    @Inject
    private Velope velope;
    @Inject
    private VelopeConfig velopeConfig;

    @Subscribe
    public void onPlayerKick(KickedFromServerEvent event) {
        String currentServerName = event.getServer().getServerInfo().getName();

        RegisteredServer destination;
        if (currentServerName == null) {
            destination = findWithBalancer(
                    velope.getProxyServer(),
                    velopedServerRepository.getServer(velopeConfig.getRootGroup()));
        } else {
            destination = findNearestAvailable(
                    velope.getProxyServer(),
                    velopedServerRepository.findParent(currentServerName)
                            .map(VelopedServer::getParent)
                            .orElse(null));

            if (destination == null
                    && Optional.ofNullable(velopeConfig.isRedirectIfUnknownEnabled()).orElse(true)) {
                destination = findWithBalancer(
                        velope.getProxyServer(),
                        velopedServerRepository.getServer(velopeConfig.getRootGroup()));
            }
        }

        if (destination == null) {
            return;
        }

        event.setResult(KickedFromServerEvent.RedirectPlayer.create(
                destination,
                event.getServerKickReason().orElse(Component.empty())));
    }
}

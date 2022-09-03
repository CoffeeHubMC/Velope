package me.theseems.velope.listener;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.KickedFromServerEvent;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import me.theseems.velope.Velope;
import me.theseems.velope.config.user.VelopeConfig;
import me.theseems.velope.history.RedirectEntry;
import me.theseems.velope.history.RedirectHistoryRepository;
import me.theseems.velope.server.VelopedServer;
import me.theseems.velope.server.VelopedServerRepository;
import me.theseems.velope.utils.ConnectionUtils;
import net.kyori.adventure.text.Component;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static me.theseems.velope.utils.ConnectionUtils.findNearestAvailable;
import static me.theseems.velope.utils.ConnectionUtils.findWithBalancer;

public class VelopeServerKickListener {
    @Inject
    private VelopedServerRepository serverRepository;
    @Inject
    private RedirectHistoryRepository historyRepository;
    @Inject
    private Velope velope;
    @Inject
    private VelopeConfig velopeConfig;

    @Subscribe
    public void onPlayerKick(KickedFromServerEvent event) {
        String currentServerName = event.getServer().getServerInfo().getName();
        Set<String> excluded = new HashSet<>(ConnectionUtils.getExclusionListForPlayer(event.getPlayer()));

        RegisteredServer destination;
        if (currentServerName == null) {
            destination = findWithBalancer(
                    velope.getProxyServer(),
                    serverRepository.getServer(velopeConfig.getRootGroup()),
                    excluded);
        } else {
            excluded.add(currentServerName);

            destination = findNearestAvailable(
                    velope.getProxyServer(),
                    serverRepository.findParent(currentServerName)
                            .map(VelopedServer::getParent)
                            .orElse(null),
                    excluded);

            if (destination == null
                    && Optional.ofNullable(velopeConfig.isRedirectIfUnknownEnabled()).orElse(true)) {
                destination = findWithBalancer(
                        velope.getProxyServer(),
                        serverRepository.getServer(velopeConfig.getRootGroup()),
                        excluded);
            }
        }

        if (destination == null) {
            return;
        }

        historyRepository.setLatestRedirect(new RedirectEntry(
                event.getPlayer().getUniqueId(),
                null,
                destination.getServerInfo().getName()
        ));
        event.setResult(KickedFromServerEvent.RedirectPlayer.create(
                destination,
                event.getServerKickReason().orElse(Component.empty())));
    }
}

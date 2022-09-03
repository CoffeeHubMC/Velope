package me.theseems.velope.listener.support;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import me.theseems.velope.Velope;
import me.theseems.velope.history.RedirectEntry;
import me.theseems.velope.history.RedirectHistoryRepository;
import me.theseems.velope.server.VelopedServer;
import me.theseems.velope.server.VelopedServerRepository;
import me.theseems.velope.utils.ConnectionUtils;

import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

public class AdvancedPortalsSupportMessageListener {
    private static final String ADVANCED_PORTALS_CHANNEL_ID = "advancedportals:warp";
    private static final String PORTAL_ENTER_SUBCHANNEL = "PortalEnter";

    @Inject
    private Velope velope;
    @Inject
    private RedirectHistoryRepository historyRepository;
    @Inject
    private VelopedServerRepository serverRepository;

    @Subscribe
    public void onPluginMessage(PluginMessageEvent event) {
        if (event.getIdentifier().getId().equals(ADVANCED_PORTALS_CHANNEL_ID)) {
            ByteArrayDataInput inputStream = ByteStreams.newDataInput(event.getData());
            String subChannel = inputStream.readUTF();

            if (subChannel.equals(PORTAL_ENTER_SUBCHANNEL)) {
                String targetServer = inputStream.readUTF();
                if (serverRepository.getServer(targetServer) == null) {
                    return;
                }

                String targetDestination = inputStream.readUTF();
                String targetUUID = inputStream.readUTF();
                UUID playerUUID = UUID.fromString(targetUUID);

                velope.getLogger().info(
                        String.format(
                                "Received AdvancedPortals message: [%s,%s,%s,%s] source=%s, target=%s",
                                PORTAL_ENTER_SUBCHANNEL,
                                targetServer,
                                targetDestination,
                                targetUUID,
                                event.getSource(),
                                event.getTarget()));

                Optional<Player> playerOptional = velope.getProxyServer().getPlayer(playerUUID);
                if (playerOptional.isEmpty()) {
                    velope.getLogger()
                            .warn("Could not find player with UUID '" + targetUUID + "' to handle AdvancedPortals message");
                    return;
                }

                Player player = playerOptional.get();
                VelopedServer server = serverRepository.getServer(targetServer);

                RegisteredServer registeredServer = ConnectionUtils.findNearestAvailable(
                        velope.getProxyServer(),
                        server,
                        Collections.emptyList());

                historyRepository.setLatestRedirect(new RedirectEntry(
                        playerUUID,
                        null,
                        registeredServer.getServerInfo().getName()));

                velope.getProxyServer().getEventManager()
                        .fire(new PluginMessageEvent(
                                event.getSource(),
                                event.getTarget(),
                                event.getIdentifier(),
                                makeData(registeredServer.getServerInfo().getName(),
                                        targetDestination,
                                        targetUUID)));

                player.createConnectionRequest(registeredServer)
                        .connect();

                event.setResult(PluginMessageEvent.ForwardResult.handled());
            }
        }
    }

    private byte[] makeData(String targetServer,
                            String targetDestination,
                            String targetUUID) {
        velope.getLogger().info(
                String.format("Baking message: [%s,%s,%s,%s]",
                        PORTAL_ENTER_SUBCHANNEL,
                        targetServer,
                        targetDestination,
                        targetUUID));

        ByteArrayDataOutput stream = ByteStreams.newDataOutput();
        stream.writeUTF(PORTAL_ENTER_SUBCHANNEL);
        stream.writeUTF(targetServer);
        stream.writeUTF(targetDestination);
        stream.writeUTF(targetUUID);

        return stream.toByteArray();
    }
}

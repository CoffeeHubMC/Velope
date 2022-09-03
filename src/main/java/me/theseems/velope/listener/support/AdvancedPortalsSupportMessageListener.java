package me.theseems.velope.listener.support;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.messages.ChannelIdentifier;
import me.theseems.velope.Velope;
import me.theseems.velope.history.RedirectHistoryRepository;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Optional;
import java.util.UUID;

public class AdvancedPortalsSupportMessageListener {
    private static final String ADVANCED_PORTALS_CHANNEL_ID = "advancedportals:warp";
    private static final String PORTAL_ENTER_SUBCHANNEL = "PortalEnter";

    @Inject
    private Velope velope;
    @Inject
    private RedirectHistoryRepository repository;

    @Subscribe
    public void onPluginMessage(PluginMessageEvent event) {
        if (event.getIdentifier().getId().equals(ADVANCED_PORTALS_CHANNEL_ID)) {
            try (ObjectInputStream inputStream = new ObjectInputStream(event.dataAsInputStream())) {
                String subChannel = inputStream.readUTF();

                if (subChannel.equals(PORTAL_ENTER_SUBCHANNEL)) {
                    String targetServer = inputStream.readUTF();
                    String targetDestination = inputStream.readUTF();  // Target destination
                    String targetUUID = inputStream.readUTF();

                    velope.getLogger().info(
                            String.format(
                                    "Received AdvancedPortals message: [%s,%s,%s,%s]",
                                    PORTAL_ENTER_SUBCHANNEL,
                                    targetServer,
                                    targetDestination,
                                    targetUUID));

                    repository.getLatestRedirect(UUID.fromString(targetUUID))
                            .ifPresentOrElse(redirectEntry -> {
                                String realTargetDestination = redirectEntry.getTo();
                                Optional<Player> playerOptional = velope.getProxyServer().getPlayer(targetUUID);
                                if (playerOptional.isEmpty()) {
                                    velope.getLogger()
                                            .warn("Could not find player with UUID '" + targetUUID + "' to handle AdvancedPortals message");
                                    return;
                                }

                                event.setResult(PluginMessageEvent.ForwardResult.handled());
                                emmitPortalEnter(event.getIdentifier(),
                                        playerOptional.get(),
                                        targetServer,
                                        realTargetDestination,
                                        targetUUID);
                            }, () -> velope.getLogger()
                                    .warn("Could not find recent redirect for player of UUID '" + targetUUID + "'."));
                }
            } catch (IOException e) {
                velope.getLogger()
                        .warn("An error occurred while reading data from AdvancedPortal's message: " + e.getMessage());
                throw new RuntimeException(e);
            }
        }
    }

    private void emmitPortalEnter(ChannelIdentifier identifier,
                                  Player player,
                                  String targetServer,
                                  String targetDestination,
                                  String targetUUID) {
        velope.getLogger().info(
                String.format("Emmitting message on %s: [%s,%s,%s,%s]",
                        identifier.getId(),
                        PORTAL_ENTER_SUBCHANNEL,
                        targetServer,
                        targetDestination,
                        targetUUID));

        ByteArrayDataOutput stream = ByteStreams.newDataOutput();
        stream.writeUTF(PORTAL_ENTER_SUBCHANNEL);
        stream.writeUTF(targetServer);
        stream.writeUTF(targetDestination);
        stream.writeUTF(targetUUID);

        player.sendPluginMessage(identifier, stream.toByteArray());
    }
}

package me.theseems.velope.commands;

import com.google.inject.Inject;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerInfo;
import me.theseems.velope.Velope;
import me.theseems.velope.config.user.VelopeConfig;
import me.theseems.velope.server.VelopedServer;
import me.theseems.velope.server.VelopedServerRepository;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import static me.theseems.velope.utils.ConnectionUtils.*;

public class LobbyCommand implements SimpleCommand {
    @Inject
    private VelopeConfig velopeConfig;
    @Inject
    private VelopedServerRepository velopedServerRepository;
    @Inject
    private Velope velope;

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        if (!(source instanceof Player)) {
            source.sendMessage(Component.text("This command is only for ingame use."));
            return;
        }

        Player player = (Player) source;
        String currentServerName = player.getCurrentServer()
                .map(ServerConnection::getServerInfo)
                .map(ServerInfo::getName)
                .orElse(null);

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
        }

        if (destination == null) {
            source.sendMessage(Component.text("There is nowhere to go").color(NamedTextColor.RED));
            return;
        }

        connectAndSupervise(player, destination);
    }
}

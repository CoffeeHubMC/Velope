package me.theseems.velope.commands;

import com.google.inject.Inject;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerInfo;
import me.theseems.velope.Velope;
import me.theseems.velope.config.user.VelopeConfig;
import me.theseems.velope.server.VelopedServer;
import me.theseems.velope.server.VelopedServerRepository;
import me.theseems.velope.status.ServerStatus;
import me.theseems.velope.status.ServerStatusRepository;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class StatusCommand implements SimpleCommand {
    public static final String STATUS_COMMAND_USE_PERMISSION = "velope.status.use";

    @Inject
    private VelopeConfig velopeConfig;
    @Inject
    private VelopedServerRepository serverRepository;
    @Inject
    private ServerStatusRepository statusRepository;
    @Inject
    private Velope velope;
    @Inject
    private MiniMessage miniMessage;

    private Component describeServerInfo(ServerInfo serverInfo) {
        Optional<ServerStatus> serverStatusOptional = statusRepository.getStatus(serverInfo.getName());
        if (serverStatusOptional.isEmpty()) {
            return miniMessage.deserialize(
                    "<red>[" + serverInfo.getName() + "]</red> - <red>Unknown</red>");
        }

        ServerStatus status = serverStatusOptional.get();
        return miniMessage.deserialize(
                "<click:run_command:vstatus " + serverInfo.getName() + ">" +
                        (status.isAvailable()
                                ? "<green>" + serverInfo.getName() + " (" + status.getPlayerCount() + "/" + status.getMaxPlayerCount() + ")</green>"
                                : "<yellow>" + serverInfo.getName() + " (Unavailable)</yellow>")
                        + "</click>"
        );
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource sender = invocation.source();
        if (!sender.hasPermission(STATUS_COMMAND_USE_PERMISSION)) {
            sender.sendMessage(Component
                    .text("You don't have permission to use that command.")
                    .color(NamedTextColor.RED));
            return;
        }

        String[] args = invocation.arguments();
        if (args.length == 0) {
            sender.sendMessage(Component.text("Please, enter the server name").color(NamedTextColor.RED));
            return;
        }

        String serverName = args[0];
        VelopedServer server = serverRepository.getServer(serverName);
        if (server != null) {
            List<Component> components = server.getGroup().stream()
                    .map(this::describeServerInfo)
                    .collect(Collectors.toList());

            Component result =
                    miniMessage.deserialize(String.format(
                                    "<gray>---------</gray> <bold><yellow>%s</yellow></bold> <gray>---------</gray>",
                                    serverName))
                            .append(Component.newline())
                            .append(miniMessage.deserialize(String.format(
                                    "<gray>Contains </gray><yellow>%d</yellow> <gray>server(-s)</gray>",
                                    server.getGroup().size())));

            for (Component component : components) {
                result = result.append(Component.newline()).append(component);
            }

            sender.sendMessage(result);
        } else {
            velope.getProxyServer().getServer(serverName)
                    .map(RegisteredServer::getServerInfo)
                    .ifPresentOrElse(
                            serverInfo -> sender.sendMessage(
                                    miniMessage.deserialize(String.format("<gray>---------</gray> %s <gray>---------</gray>", serverName))
                                            .append(Component.newline())
                                            .append(describeServerInfo(serverInfo))),
                            () -> sender.sendMessage(
                                    Component.text("Could not find desired server (neither regular nor veloped)")
                                            .color(NamedTextColor.RED)));
        }
    }
}

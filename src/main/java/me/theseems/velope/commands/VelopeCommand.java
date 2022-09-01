package me.theseems.velope.commands;

import com.google.inject.Inject;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.server.ServerInfo;
import me.theseems.velope.BuildConstants;
import me.theseems.velope.Velope;
import me.theseems.velope.server.VelopedServer;
import me.theseems.velope.server.VelopedServerRepository;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.Collection;

public class VelopeCommand implements SimpleCommand {
    public static final String LIST_SUBCOMMAND_USE_PERMISSION = "velope.list";
    public static final String RELOAD_SUBCOMMAND_USE_PERMISSION = "velope.reload";

    @Inject
    private VelopedServerRepository serverRepository;
    @Inject
    private Velope velope;
    @Inject
    private MiniMessage miniMessage;

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        String[] args = invocation.arguments();

        if (args.length == 0
                || args[0].equalsIgnoreCase("about")
                || args[0].equalsIgnoreCase("help")) {
            sendAbout(source);
            return;
        }

        switch (args[0].toLowerCase()) {
            case "list":
                if (!source.hasPermission(LIST_SUBCOMMAND_USE_PERMISSION)) {
                    source.sendMessage(Component
                            .text("You don't have permission to use that command.")
                            .color(NamedTextColor.RED));
                    return;
                }

                Collection<VelopedServer> servers = serverRepository.findAll();
                source.sendMessage(miniMessage.deserialize(String.format(
                        "<gray>There are </gray><yellow>%d</yellow><gray> veloped server(-s)</gray>",
                        servers.size())));

                String currentServerName = null;
                if (source instanceof Player) {
                    currentServerName = ((Player) source).getCurrentServer()
                            .map(ServerConnection::getServerInfo)
                            .map(ServerInfo::getName)
                            .map(serverName -> serverRepository.getServer(serverName))
                            .map(VelopedServer::getName)
                            .orElse(null);
                }

                String finalCurrentServerName = currentServerName;
                Component result = servers.stream()
                        .map(velopedServer ->
                                Component.text(velopedServer.getName())
                                        .color(velopedServer.getName().equals(finalCurrentServerName)
                                                ? NamedTextColor.YELLOW
                                                : NamedTextColor.GRAY)
                                        .append(velopedServer.getParent() != null
                                                ? Component.text(" (/\\ " + velopedServer.getParent().getName() + ")")
                                                : Component.empty()))
                        .reduce(Component.empty(),
                                (first, second) -> first
                                        .append(Component.space())
                                        .append(Component.space())
                                        .append(second));

                source.sendMessage(result);
                break;

            case "reload":
                if (!source.hasPermission(RELOAD_SUBCOMMAND_USE_PERMISSION)) {
                    source.sendMessage(Component
                            .text("You don't have permission to use that command.")
                            .color(NamedTextColor.RED));
                    return;
                }

                try {
                    velope.reload();
                    source.sendMessage(Component
                            .text("Velope is reloaded.")
                            .color(NamedTextColor.YELLOW));
                } catch (Exception e) {
                    source.sendMessage(Component
                            .text("Failed to reload plugin: " + e.getMessage())
                            .color(NamedTextColor.RED));
                    e.printStackTrace();
                }
                break;

            default:
                sendAbout(source);
                break;
        }
    }

    private void sendAbout(CommandSource source) {
        source.sendMessage(
                LegacyComponentSerializer.legacyAmpersand().deserialize(
                                "&8&m         &r &e&lVelope v" + BuildConstants.VERSION + " &8&m         &r")
                        .append(Component.newline())
                        .append(LegacyComponentSerializer.legacyAmpersand()
                                .deserialize("&7Velocity Plugin for simple balancing & server organising."))
                        .append(Component.newline())
                        .append(source.hasPermission(LIST_SUBCOMMAND_USE_PERMISSION)
                                ? miniMessage.deserialize("<yellow>/velope list</yellow>")
                                .append(Component.newline())
                                : Component.empty())
                        .append(source.hasPermission(RELOAD_SUBCOMMAND_USE_PERMISSION)
                                ? miniMessage.deserialize("<yellow>/velope reload</yellow>")
                                .append(Component.newline())
                                : Component.empty())
                        .append(source.hasPermission(StatusCommand.STATUS_COMMAND_USE_PERMISSION)
                                ? LegacyComponentSerializer.legacyAmpersand()
                                .deserialize("&e/vstatus <server_name>")
                                : Component.empty()));
    }
}

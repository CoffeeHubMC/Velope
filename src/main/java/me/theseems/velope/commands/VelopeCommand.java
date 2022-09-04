package me.theseems.velope.commands;

import com.google.inject.Inject;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.server.ServerInfo;
import me.theseems.velope.BuildConstants;
import me.theseems.velope.Velope;
import me.theseems.velope.history.RedirectEntry;
import me.theseems.velope.history.RedirectHistoryRepository;
import me.theseems.velope.server.VelopedServer;
import me.theseems.velope.server.VelopedServerRepository;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class VelopeCommand implements SimpleCommand {
    public static final String LIST_SUBCOMMAND_USE_PERMISSION = "velope.list";
    public static final String RELOAD_SUBCOMMAND_USE_PERMISSION = "velope.reload";
    public static final String RECENT_SUBCOMMAND_USE_PERMISSION = "velope.recent";

    @Inject
    private VelopedServerRepository serverRepository;
    @Inject
    private RedirectHistoryRepository historyRepository;
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
                        "<gray>Recognized veloped server(s): </gray><yellow>%d</yellow>",
                        servers.size())));

                String currentServerName = null;
                if (source instanceof Player) {
                    currentServerName = ((Player) source).getCurrentServer()
                            .map(ServerConnection::getServerInfo)
                            .map(ServerInfo::getName)
                            .map(serverName -> serverRepository.getParent(serverName))
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
                                        .clickEvent(ClickEvent.clickEvent(
                                                ClickEvent.Action.RUN_COMMAND,
                                                "/vstatus " + velopedServer.getName()))
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

            case "recent":
                if (!source.hasPermission(RECENT_SUBCOMMAND_USE_PERMISSION)) {
                    source.sendMessage(Component
                            .text("You don't have permission to use that command.")
                            .color(NamedTextColor.RED));
                    return;
                }
                if (args.length == 1) {
                    source.sendMessage(Component
                            .text("Please, specify the player's name.")
                            .color(NamedTextColor.RED));
                    return;
                }

                String playerName = args[1];
                Optional<Player> optionalPlayer = velope.getProxyServer().getPlayer(playerName);
                if (optionalPlayer.isEmpty()) {
                    source.sendMessage(Component
                            .text("Requested player is not found.")
                            .color(NamedTextColor.RED));
                    return;
                }

                UUID playerUUID = optionalPlayer.get().getUniqueId();
                Optional<RedirectEntry> entryOptional = historyRepository.getLatestRedirect(playerUUID);

                if (entryOptional.isEmpty()) {
                    source.sendMessage(Component
                            .text("No entries found.")
                            .color(NamedTextColor.RED));
                    return;
                }

                RedirectEntry entry = entryOptional.get();

                LegacyComponentSerializer serializer = LegacyComponentSerializer.legacyAmpersand();
                source.sendMessage(
                        serializer.deserialize("&7Latest Velope redirect:").append(Component.newline())
                                .append(serializer.deserialize("&8\u22D9 &7From: &e" +
                                        (entry.getFrom() == null ? "<root>" : entry.getFrom().getName())))
                                .append(Component.newline())
                                .append(serializer.deserialize("&8\u22D8 &7To: &e" +
                                        (entry.getTo() == null ? "<void>" : entry.getTo()))));

                Map<String, Long> failureMap = historyRepository.getFailureMap(playerUUID);
                if (failureMap == null || failureMap.isEmpty()) {
                    return;
                }

                source.sendMessage(serializer.deserialize("&7Failed servers:"));
                failureMap.forEach((s, aLong) -> source.sendMessage(
                        serializer.deserialize("&e" + s + " &8- &e" + aLong + " &7time(s)")
                ));
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
                        .append(source.hasPermission(RECENT_SUBCOMMAND_USE_PERMISSION)
                                ? LegacyComponentSerializer.legacyAmpersand()
                                .deserialize("&e/velope recent <player_name>")
                                .append(Component.newline())
                                : Component.empty())
                        .append(source.hasPermission(StatusCommand.STATUS_COMMAND_USE_PERMISSION)
                                ? LegacyComponentSerializer.legacyAmpersand()
                                .deserialize("&e/vstatus <server_name>")
                                : Component.empty()));
    }
}

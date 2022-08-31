package me.theseems.velope;

import com.google.common.io.Files;
import com.google.gson.Gson;
import com.google.inject.*;
import com.google.inject.name.Names;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerInfo;
import com.velocitypowered.api.proxy.server.ServerPing;
import me.theseems.velope.commands.LobbyCommand;
import me.theseems.velope.commands.StatusCommand;
import me.theseems.velope.commands.VelopeCommand;
import me.theseems.velope.config.code.AdventureConfig;
import me.theseems.velope.config.code.PluginConfig;
import me.theseems.velope.config.code.RepositoryConfig;
import me.theseems.velope.config.code.VelopeUserConfig;
import me.theseems.velope.config.user.VelopeCommandConfig;
import me.theseems.velope.config.user.VelopeConfig;
import me.theseems.velope.config.user.VelopeGroupConfig;
import me.theseems.velope.handler.ServerPingerHandler;
import me.theseems.velope.listener.VelopeServerInitialListener;
import me.theseems.velope.listener.VelopeServerJoinListener;
import me.theseems.velope.listener.VelopeServerKickListener;
import me.theseems.velope.server.VelopedServer;
import me.theseems.velope.server.VelopedServerRepository;
import me.theseems.velope.status.ServerStatus;
import me.theseems.velope.status.ServerStatusRepository;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.slf4j.Logger;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static me.theseems.velope.utils.ConnectionUtils.connectAndSupervise;

@Plugin(
        id = "velope",
        name = "Velope",
        version = BuildConstants.VERSION,
        description = "A simple hirerchial balancer and fallback plugin",
        authors = {"theseems"}
)
public class Velope {

    private final Logger logger;
    private final ProxyServer proxyServer;
    private final Path pluginFolder;
    private static Velope velope;
    private static Injector injector;
    private static VelopeConfig velopeConfig;
    private static RepositorySetup repositorySetup;

    @Inject
    public Velope(Logger logger, ProxyServer proxyServer, @DataDirectory Path pluginFolder) {
        this.logger = logger;
        this.proxyServer = proxyServer;
        this.pluginFolder = pluginFolder;
        velope = this;
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        try {
            velopeConfig = loadConfig();
        } catch (Exception e) {
            logger.error("Failed to load config. Quitting...");
            return;
        }
        if (velopeConfig == null) {
            logger.error("Please, fill in the config. Quitting...");
            return;
        }

        // Register modules
        injector = Guice.createInjector(
                new RepositoryConfig(),
                new PluginConfig(),
                new VelopeUserConfig(),
                new AdventureConfig());

        // Setup repositories
        repositorySetup = injector.getInstance(RepositorySetup.class);
        repositorySetup.setup();

        // Register listeners
        proxyServer.getEventManager().register(this, injector.getInstance(VelopeServerJoinListener.class));
        proxyServer.getEventManager().register(this, injector.getInstance(VelopeServerKickListener.class));

        // Register commands
        CommandManager manager = proxyServer.getCommandManager();
        manager.register(
                manager.metaBuilder("lobby")
                        .aliases("leave", "back")
                        .build(),
                injector.getInstance(LobbyCommand.class));
        manager.register(
                manager.metaBuilder("velope").build(),
                injector.getInstance(VelopeCommand.class));
        manager.register(
                manager.metaBuilder("vstatus").build(),
                injector.getInstance(StatusCommand.class));

        // Register handler
        proxyServer.getScheduler()
                .buildTask(this, injector.getInstance(ServerPingerHandler.class))
                .repeat(10, TimeUnit.SECONDS)
                .schedule();
    }

    private VelopeConfig loadConfig() throws IOException {
        File pluginFolder = this.pluginFolder.toFile();
        if (!pluginFolder.exists()) {
            pluginFolder.mkdir();
        }

        File config = new File(pluginFolder, "config.json");
        if (!config.exists()) {
            config.createNewFile();
            try (InputStream stream = getClass().getClassLoader().getResourceAsStream("config.json")) {
                if (stream == null) {
                    logger.error("Failed to fill in empty config." +
                            " Configure plugin from scratch manually." +
                            " For now using blank configuration");
                    return new VelopeConfig();
                }

                byte[] buffer = new byte[stream.available()];
                stream.read(buffer);
                Files.write(buffer, config);
            }
        }

        try {
            return new Gson().fromJson(new FileReader(config), VelopeConfig.class);
        } catch (Exception e) {
            logger.error("Failed to read and/or construct config: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    public void reload() throws IOException {
        repositorySetup.clear();
        injector.getInstance(ServerStatusRepository.class).deleteAll();
        injector.getInstance(VelopedServerRepository.class).deleteAll();

        velopeConfig = loadConfig();
        if (velopeConfig == null) {
            velopeConfig = new VelopeConfig();
            logger.error("Please, fill in the config. For now using blank one.");
        }

        logger.info("Parsed groups (initially): " + velopeConfig.getGroups().stream()
                .map(VelopeGroupConfig::getName)
                .collect(Collectors.joining(",")));

        repositorySetup = injector.getInstance(RepositorySetup.class);

        // Setup repositories
        repositorySetup.setup();
    }

    public Logger getLogger() {
        return logger;
    }

    public ProxyServer getProxyServer() {
        return proxyServer;
    }

    public static class RepositorySetup {
        @Inject
        private VelopedServerRepository velopedServerRepository;
        @Inject
        private VelopeConfig velopeConfig;
        @Inject
        private Velope velope;
        @Inject
        private ServerStatusRepository serverStatusRepository;

        private final List<RegisteredServer> registeredServers = new ArrayList<>();
        private final List<String> registeredCommandLabels = new ArrayList<>();
        private final List<Object> listeners = new ArrayList<>();

        private void fetchServers(Map<String, VelopedServer> velopedServerMap) {
            int index = 8000;

            for (VelopeGroupConfig group : velopeConfig.getGroups()) {
                index++;

                List<ServerInfo> serverInfoList = new ArrayList<>();
                for (String server : group.getServers()) {
                    RegisteredServer registeredServer = velope.getProxyServer().getServer(server).orElse(null);
                    if (registeredServer == null) {
                        velope.getLogger().error("Server '" + server + "' was not found");
                        continue;
                    }

                    serverInfoList.add(registeredServer.getServerInfo());
                    registeredServer.ping().whenCompleteAsync((serverPing, throwable) ->
                            serverStatusRepository.saveStatus(
                                    new ServerStatus(
                                            registeredServer.getServerInfo(),
                                            serverPing.getDescriptionComponent(),
                                            throwable == null,
                                            serverPing.getPlayers().map(ServerPing.Players::getOnline).orElse(0),
                                            serverPing.getPlayers().map(ServerPing.Players::getMax).orElse(0)
                                    )
                            ));
                }

                RegisteredServer registeredServer = velope.getProxyServer().registerServer(
                        new ServerInfo(
                                group.getName(),
                                InetSocketAddress.createUnresolved("fake.server.balancer", index++)
                        )
                );

                registeredServers.add(registeredServer);

                VelopedServer server = new VelopedServer(
                        group.getName(),
                        registeredServer,
                        group.getBalanceStrategy(),
                        null,
                        serverInfoList);
                velopedServerMap.put(server.getName(), server);
            }
        }

        private void linkParents(Map<String, VelopedServer> velopedServerMap) {
            for (VelopeGroupConfig group : velopeConfig.getGroups()) {
                if (group.getParent() == null) {
                    continue;
                }

                String parent = group.getParent();
                if (!velopedServerMap.containsKey(parent)) {
                    velope.getLogger().warn("Parent '" + parent + "' for '" + group.getName() + "' does not exist");
                    continue;
                }

                if (velopedServerMap.get(group.getName()).getParent() != null) {
                    velope.getLogger().warn(String.format("Group '%s' already has parent '%s'",
                            group.getName(), velopedServerMap.get(group.getName()).getParent().getName()));
                    continue;
                }

                velopedServerMap.get(group.getName()).setParent(velopedServerMap.get(parent));
            }
        }

        private void saveToRepository(Map<String, VelopedServer> velopedServerMap) {
            for (VelopedServer value : velopedServerMap.values()) {
                velopedServerRepository.addServer(value);
            }
        }

        private void registerGroupCommands(Map<String, VelopedServer> velopedServerMap) {
            for (VelopeGroupConfig group : velopeConfig.getGroups()) {
                if (group.getCommand() == null) {
                    continue;
                }
                if (!velopedServerMap.containsKey(group.getName())) {
                    velope.getLogger().warn(String.format(
                            "Command for group '%s' has not been registered because group was not interpreted",
                            group.getName()));
                    continue;
                }

                VelopedServer velopedServer = velopedServerMap.get(group.getName());

                VelopeCommandConfig config = group.getCommand();
                CommandManager commandManager = velope.getProxyServer().getCommandManager();
                CommandMeta commandMeta = commandManager.metaBuilder(config.getLabel())
                        .aliases(config.getAliases().toArray(String[]::new))
                        .build();

                SimpleCommand command = invocation -> {
                    if (!(invocation.source() instanceof Player)) {
                        invocation.source().sendMessage(Component.text("This game is only for ingame use."));
                        return;
                    }
                    if (config.getPermission() != null && !invocation.source().hasPermission(config.getPermission())) {
                        invocation.source().sendMessage(Component
                                .text("You don't have permission to use that command.")
                                .color(NamedTextColor.RED));
                        return;
                    }

                    Optional<VelopedServer> currentServer = ((Player) invocation.source())
                            .getCurrentServer()
                            .map(ServerConnection::getServer)
                            .map(registeredServer -> velopedServerRepository
                                    .getParent(registeredServer.getServerInfo().getName()));

                    if (currentServer.isPresent() && currentServer.get().getName().equals(velopedServer.getName())) {
                        invocation.source().sendMessage(Component
                                .text("You are already at your destination.")
                                .color(NamedTextColor.RED));
                        return;
                    }

                    connectAndSupervise(velope.getProxyServer(), (Player) invocation.source(), velopedServer);
                };

                commandManager.register(commandMeta, command);
                registeredCommandLabels.add(config.getLabel());
                registeredCommandLabels.addAll(config.getAliases());
            }
        }

        private void setupInitialListener(Map<String, VelopedServer> velopedServerMap) {
            String group = velopeConfig.getInitialGroup();
            if (velopeConfig.getInitialGroup() == null) {
                return;
            }
            if (!velopedServerMap.containsKey(group)) {
                velope.getLogger().warn("Unknown or incorrectly parsed group set for initial: '" + group + "'");
                return;
            }

            VelopedServer server = velopedServerMap.get(group);
            VelopeServerInitialListener listener = injector.createChildInjector(new AbstractModule() {
                @Override
                protected void configure() {
                    bind(VelopedServer.class)
                            .annotatedWith(Names.named("initial"))
                            .toInstance(server);
                }
            }).getInstance(VelopeServerInitialListener.class);

            listeners.add(listener);
            velope.getProxyServer().getEventManager().register(velope, listener);
        }

        public void setup() {
            Map<String, VelopedServer> velopedServerMap = new HashMap<>();
            fetchServers(velopedServerMap);
            linkParents(velopedServerMap);
            saveToRepository(velopedServerMap);
            registerGroupCommands(velopedServerMap);
            setupInitialListener(velopedServerMap);

            velope.getLogger().info("Registered balancer servers: " + velopedServerRepository.findAll().size());
        }

        public void clear() {
            for (String registeredCommandLabel : registeredCommandLabels) {
                velope.getProxyServer().getCommandManager().unregister(registeredCommandLabel);
            }
            for (RegisteredServer registeredServer : registeredServers) {
                velope.getProxyServer().unregisterServer(registeredServer.getServerInfo());
            }
            for (Object listener : listeners) {
                velope.getProxyServer().getEventManager().unregisterListener(velope, listener);
            }
            listeners.clear();
            registeredCommandLabels.clear();
            registeredServers.clear();
        }
    }

    public static Velope getInstance() {
        return velope;
    }

    public static ServerStatusRepository getStatusRepository() {
        return injector.getInstance(ServerStatusRepository.class);
    }

    public static VelopedServerRepository getServerRepository() {
        return injector.getInstance(VelopedServerRepository.class);
    }

    public static VelopeConfig getVelopeConfig() {
        return velopeConfig;
    }
}

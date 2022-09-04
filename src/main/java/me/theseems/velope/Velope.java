package me.theseems.velope;

import com.google.common.io.Files;
import com.google.gson.Gson;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.name.Names;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerInfo;
import com.velocitypowered.api.proxy.server.ServerPing;
import com.velocitypowered.api.scheduler.ScheduledTask;
import me.theseems.velope.commands.LobbyCommand;
import me.theseems.velope.commands.StatusCommand;
import me.theseems.velope.commands.VelopeCommand;
import me.theseems.velope.config.code.AdventureConfig;
import me.theseems.velope.config.code.PluginConfig;
import me.theseems.velope.config.code.RepositoryConfig;
import me.theseems.velope.config.code.VelopeUserConfig;
import me.theseems.velope.config.user.*;
import me.theseems.velope.handler.FailureHistoryWipeHandler;
import me.theseems.velope.handler.ServerPingerHandler;
import me.theseems.velope.history.RedirectHistoryRepository;
import me.theseems.velope.listener.history.HistoryDisconnectListener;
import me.theseems.velope.listener.support.AdvancedPortalsSupportMessageListener;
import me.theseems.velope.listener.velope.VelopeServerInitialListener;
import me.theseems.velope.listener.velope.VelopeServerJoinListener;
import me.theseems.velope.listener.velope.VelopeServerKickListener;
import me.theseems.velope.server.VelopedServer;
import me.theseems.velope.server.VelopedServerRepository;
import me.theseems.velope.status.ServerStatus;
import me.theseems.velope.status.ServerStatusRepository;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.slf4j.Logger;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.time.Duration;
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
    private static final long CACHE_TTL_HARD_MIN = 3000L;
    private static final long CACHE_TTL_DEFAULT = 10_000L;

    private static final long PINGER_INTERVAL_HARD_MIN = 3000L;
    private static final long PINGER_INTERVAL_DEFAULT = 10_000L;

    private static final long LOG_UNAVAILABLE_COOLDOWN_HARD_MIN = 3000L;
    private static final long LOG_UNAVAILABLE_COOLDOWN_DEFAULT = 120_000L;

    private static final long WIPER_INTERVAL_HARD_MIN = 15000L;

    private static Velope velope;
    private static Injector injector;
    private static VelopeConfig velopeConfig;
    private static RepositorySetup repositorySetup;

    private final Logger logger;
    private final ProxyServer proxyServer;
    private final Path pluginFolder;
    private ScheduledTask pingerTask;
    private ScheduledTask failureWiperTask;

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

        pingerTask = setupPingerTask();
        failureWiperTask = setupFailureWipeTask();
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
        pingerTask.cancel();
        failureWiperTask.cancel();
        repositorySetup.clear();
        injector.getInstance(ServerStatusRepository.class).deleteAll();
        injector.getInstance(VelopedServerRepository.class).deleteAll();

        RedirectHistoryRepository repository = injector.getInstance(RedirectHistoryRepository.class);
        repository.cleanFailures();
        repository.cleanLatestRedirects();

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
        pingerTask = setupPingerTask();
        failureWiperTask = setupFailureWipeTask();
    }

    private ScheduledTask setupFailureWipeTask() {
        long wiperInterval = Math.max(WIPER_INTERVAL_HARD_MIN,
                velopeConfig.getVelopeFailureConfig().getFailureCleanInterval());

        return proxyServer.getScheduler()
                .buildTask(velope, injector.getInstance(FailureHistoryWipeHandler.class))
                .repeat(wiperInterval, TimeUnit.MILLISECONDS)
                .schedule();
    }

    private ScheduledTask setupPingerTask() {
        Injector handlerInjector = injector.createChildInjector(new AbstractModule() {
            @Override
            protected void configure() {
                Duration cacheTtl = Duration.ofMillis(Math.max(
                        CACHE_TTL_HARD_MIN,
                        Optional.ofNullable(velopeConfig.getPingerConfig())
                                .map(VelopePingerConfig::getCacheTtl)
                                .orElse(CACHE_TTL_DEFAULT)));
                Duration logUavailableCooldown = Duration.ofMillis(Math.max(
                        LOG_UNAVAILABLE_COOLDOWN_HARD_MIN,
                        Optional.ofNullable(velopeConfig.getPingerConfig())
                                .map(VelopePingerConfig::getLogUnavailableCooldown)
                                .orElse(LOG_UNAVAILABLE_COOLDOWN_DEFAULT)));

                bind(Duration.class)
                        .annotatedWith(Names.named("cacheTtl"))
                        .toInstance(cacheTtl);

                bind(Duration.class)
                        .annotatedWith(Names.named("logUnavailableCooldown"))
                        .toInstance(logUavailableCooldown);
            }
        });

        long pingerInterval = Math.max(
                PINGER_INTERVAL_HARD_MIN,
                Optional.ofNullable(velopeConfig.getPingerConfig())
                        .map(VelopePingerConfig::getPingInterval)
                        .orElse(PINGER_INTERVAL_DEFAULT));

        // Register handler
        return proxyServer.getScheduler()
                .buildTask(this, handlerInjector.getInstance(ServerPingerHandler.class))
                .repeat(pingerInterval, TimeUnit.MILLISECONDS)
                .schedule();
    }

    public Logger getLogger() {
        return logger;
    }

    public ProxyServer getProxyServer() {
        return proxyServer;
    }

    public static class RepositorySetup {
        @Inject
        private VelopedServerRepository serverRepository;
        @Inject
        private VelopeConfig velopeConfig;
        @Inject
        private Velope velope;
        @Inject
        private ServerStatusRepository statusRepository;

        private final List<RegisteredServer> registeredServers = new ArrayList<>();
        private final List<String> registeredCommandLabels = new ArrayList<>();
        private final List<Object> listeners = new ArrayList<>();

        private void fetchServers(Map<String, VelopedServer> velopedServerMap) {
            int index = 8000;

            for (VelopeGroupConfig group : velopeConfig.getGroups()) {
                index++;

                List<String> serverInfoList = new ArrayList<>();
                for (String server : group.getServers()) {
                    RegisteredServer registeredServer = velope.getProxyServer().getServer(server).orElse(null);
                    if (registeredServer == null) {
                        velope.getLogger().error("Server '" + server + "' is not found");
                        serverInfoList.add(server);
                        continue;
                    }

                    serverInfoList.add(registeredServer.getServerInfo().getName());
                    registeredServer.ping().whenCompleteAsync((serverPing, throwable) ->
                            statusRepository.saveStatus(
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
                serverRepository.addServer(value);
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
                if (config == null) {
                    continue;
                }
                if (config.getLabel() == null) {
                    velope.getLogger().warn("No label specified for command of group '" + group.getName() + "'");
                    continue;
                }

                CommandManager commandManager = velope.getProxyServer().getCommandManager();
                CommandMeta commandMeta = commandManager.metaBuilder(config.getLabel())
                        .aliases(
                                Optional.ofNullable(config.getAliases())
                                        .map((aliases) -> aliases.toArray(String[]::new))
                                        .orElse(new String[]{})
                        )
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

                    Player player = (Player) invocation.source();
                    Optional<VelopedServer> currentServer = player
                            .getCurrentServer()
                            .map(ServerConnection::getServer)
                            .map(registeredServer -> serverRepository
                                    .getParent(registeredServer.getServerInfo().getName()));

                    if (currentServer.isPresent() && currentServer.get().getName().equals(velopedServer.getName())) {
                        invocation.source().sendMessage(Component
                                .text("You are already at your destination.")
                                .color(NamedTextColor.RED));
                        return;
                    }

                    connectAndSupervise(velope.getProxyServer(), player, velopedServer);
                };

                commandManager.register(commandMeta, command);
                registeredCommandLabels.add(config.getLabel());
                if (config.getAliases() != null) {
                    registeredCommandLabels.addAll(config.getAliases());
                }
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

        private void enableAdvancedPortalsSupport() {
            velope.getLogger().info("Setting up AdvancedPortals message listener");
            Object listener = injector.getInstance(AdvancedPortalsSupportMessageListener.class);
            listeners.add(listener);

            velope.getProxyServer().getEventManager().register(velope, listener);
        }

        private void setupAdvancedPortalsSupport() {
            boolean pluginFound = velope.getProxyServer()
                    .getPluginManager()
                    .isLoaded("advancedportals");

            Optional.ofNullable(velopeConfig.getIntegrationsConfig())
                    .map(VelopeIntegrationConfig::isAdvancedPortalsSupportEnabled)
                    .ifPresentOrElse(
                            (value) -> {
                                if (value) {
                                    if (!pluginFound) {
                                        velope.getLogger().warn("AdvancedPortals plugin is not found @ Velocity." +
                                                " Install it there in order to enable integration (support).");
                                        return;
                                    }

                                    enableAdvancedPortalsSupport();
                                }
                            }, () -> {
                                if (pluginFound) {
                                    velope.getLogger()
                                            .info("AdvancedPortals plugin is found. Support is enabled by default." +
                                                    " You can disable it in 'integration' section @ config.");

                                    enableAdvancedPortalsSupport();
                                }
                            }
                    );
        }

        private void setupDisconnectListener() {
            Object listener = injector.getInstance(HistoryDisconnectListener.class);
            listeners.add(listener);

            velope.getProxyServer().getEventManager().register(velope, listener);
        }

        public void setup() {
            Map<String, VelopedServer> velopedServerMap = new HashMap<>();

            // Generic startup pipeline
            fetchServers(velopedServerMap);
            linkParents(velopedServerMap);
            saveToRepository(velopedServerMap);
            registerGroupCommands(velopedServerMap);
            setupDisconnectListener();
            setupInitialListener(velopedServerMap);

            // Support
            setupAdvancedPortalsSupport();

            velope.getLogger().info("Registered Veloped servers: " + serverRepository.findAll().size());
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

    public static RedirectHistoryRepository getHistoryRepository() {
        return injector.getInstance(RedirectHistoryRepository.class);
    }

    public static VelopeConfig getVelopeConfig() {
        return velopeConfig;
    }
}

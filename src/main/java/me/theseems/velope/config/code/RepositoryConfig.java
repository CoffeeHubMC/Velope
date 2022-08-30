package me.theseems.velope.config.code;

import com.google.inject.AbstractModule;
import me.theseems.velope.server.MemoryVelopedServerRepository;
import me.theseems.velope.server.VelopedServerRepository;
import me.theseems.velope.status.MemoryServerStatusRepository;
import me.theseems.velope.status.ServerStatusRepository;

public class RepositoryConfig extends AbstractModule {
    @Override
    protected void configure() {
        bind(ServerStatusRepository.class)
                .toInstance(new MemoryServerStatusRepository());
        bind(VelopedServerRepository.class)
                .toInstance(new MemoryVelopedServerRepository());
    }
}

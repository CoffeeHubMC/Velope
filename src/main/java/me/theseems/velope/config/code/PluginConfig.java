package me.theseems.velope.config.code;

import com.google.inject.AbstractModule;
import com.google.inject.Provider;
import me.theseems.velope.Velope;
import me.theseems.velope.server.MemoryVelopedServerRepository;
import me.theseems.velope.server.VelopedServerRepository;
import me.theseems.velope.status.MemoryServerStatusRepository;
import me.theseems.velope.status.ServerStatusRepository;

public class PluginConfig extends AbstractModule {
    @Override
    protected void configure() {
        bind(Velope.class).toProvider(Velope::getInstance);
    }
}

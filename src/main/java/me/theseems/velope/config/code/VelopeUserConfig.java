package me.theseems.velope.config.code;

import com.google.inject.AbstractModule;
import com.google.inject.Provider;
import me.theseems.velope.Velope;
import me.theseems.velope.config.user.VelopeConfig;
import me.theseems.velope.status.MemoryServerStatusRepository;
import me.theseems.velope.status.ServerStatusRepository;

public class VelopeUserConfig extends AbstractModule {
    @Override
    protected void configure() {
        bind(VelopeConfig.class).toProvider(Velope::getVelopeConfig);
    }
}

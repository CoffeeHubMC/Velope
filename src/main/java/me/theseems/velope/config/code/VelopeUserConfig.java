package me.theseems.velope.config.code;

import com.google.inject.AbstractModule;
import me.theseems.velope.Velope;
import me.theseems.velope.config.user.VelopeConfig;

public class VelopeUserConfig extends AbstractModule {
    @Override
    protected void configure() {
        bind(VelopeConfig.class).toProvider(Velope::getVelopeConfig);
    }
}

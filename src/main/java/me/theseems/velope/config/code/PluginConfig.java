package me.theseems.velope.config.code;

import com.google.inject.AbstractModule;
import me.theseems.velope.Velope;

public class PluginConfig extends AbstractModule {
    @Override
    protected void configure() {
        bind(Velope.class).toProvider(Velope::getInstance);
    }
}

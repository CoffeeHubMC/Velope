package me.theseems.velope.config.code;

import com.google.inject.AbstractModule;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.minimessage.tag.standard.StandardTags;

public class AdventureConfig extends AbstractModule {
    @Override
    protected void configure() {
        bind(MiniMessage.class).toInstance(
                MiniMessage.builder()
                        .tags(TagResolver.builder()
                                .resolver(StandardTags.color())
                                .resolver(StandardTags.decorations())
                                .resolver(StandardTags.gradient())
                                .resolver(StandardTags.clickEvent())
                                .build()
                        )
                        .build()
        );
    }
}

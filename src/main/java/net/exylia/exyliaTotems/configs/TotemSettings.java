package net.exylia.exyliaTotems.configs;

import net.exylia.lib.config.Comment;
import net.exylia.lib.config.Key;

/**
 * Settings stored in {@code config.yml}.
 *
 * <p>Read once at load and then accessed as fields: this plugin reacts to death
 * packets, so it must not go looking a value up by string while a player is
 * mid-save.
 */
public record TotemSettings(Totem totem) {

    public TotemSettings() {
        this(new Totem());
    }

    public record Totem(
            @Key("delay-ticks")
            @Comment("How long to wait after death before deciding which totem the player meant. Longer forgives worse pings, but holds the death open")
            int delayTicks) {

        public Totem() {
            this(3);
        }
    }

}

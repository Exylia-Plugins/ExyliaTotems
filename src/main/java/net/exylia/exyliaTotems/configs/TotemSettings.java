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
public record TotemSettings(Totem totem, Messages messages) {

    public TotemSettings() {
        this(new Totem(), new Messages());
    }

    public record Totem(
            @Key("delay-ticks")
            @Comment("How long to wait after death before deciding which totem the player meant. Longer forgives worse pings, but holds the death open")
            int delayTicks) {

        public Totem() {
            this(3);
        }
    }

    /**
     * What the player is told.
     *
     * <p>The saved message used to be written into three places in Java, so a
     * server owner could not change a word of it and the three could drift.
     */
    public record Messages(
            @Comment("Sent to a player whose totem was honoured despite their ping")
            String saved) {

        public Messages() {
            this("{primary}&lE-TOTEMS &8•&r {success}We saved you because of your ping difference.");
        }
    }
}

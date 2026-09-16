package net.exylia.exyliaTotems.configs;

import net.exylia.lib.config.Comment;

/**
 * What the player is told, stored in {@code messages.yml}.
 *
 * <p>The saved message used to be written into three places in Java, so a
 * server owner could not change a word of it and the three could drift.
 */
public record TotemMessages(
        @Comment("Sent to a player whose totem was honoured despite their ping")
        String saved) {

    public TotemMessages() {
        this("{primary}&lE-TOTEMS &8•&r {success}We saved you because of your ping difference.");
    }
}

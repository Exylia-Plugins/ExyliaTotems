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
        String saved,
        @Comment("Sent to a sender without exyliatotems.admin who runs /exyliatotem-reload")
        String noPermission) {

    public TotemMessages() {
        this("{primary}&lE-TOTEMS &8•&r {success}We saved you because of your ping difference.",
                "[sound:ENTITY_VILLAGER_NO|1.0|1.0]{primary}&lE-TOTEMS &8•&r {error}You do not have permission to do that.");
    }
}

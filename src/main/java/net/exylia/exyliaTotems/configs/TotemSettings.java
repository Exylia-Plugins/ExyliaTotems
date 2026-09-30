package net.exylia.exyliaTotems.configs;

import net.exylia.lib.config.Comment;
import net.exylia.lib.config.Key;
import net.exylia.lib.config.Languages;
import net.exylia.lib.config.Migration;

/**
 * Settings stored in {@code config.yml}.
 *
 * <p>Read once at load and then accessed as fields: this plugin reacts to death
 * packets, so it must not go looking a value up by string while a player is
 * mid-save.
 */
public record TotemSettings(
        @Comment("Language of this plugin's menus and messages. 'default' follows the language set in")
        @Comment("ExyliaLib's config.yml; en, es or pt set this plugin alone. Each one is a folder under")
        @Comment("lang/ you can edit; 'custom' holds the files this server had before languages existed.")
        String language,
        Totem totem) {

    public TotemSettings() {
        this(Languages.DEFAULT, new Totem());
    }

    /** Version 1 to 2: a server that ran the plugin before languages keeps its files, in {@code lang/custom/}. */
    public static final Migration MIGRATION_FROM_1 = Languages.ADOPT_EXISTING;

    public record Totem(
            @Key("delay-ticks")
            @Comment("How long to wait after death before deciding which totem the player meant. Longer forgives worse pings, but holds the death open")
            int delayTicks) {

        public Totem() {
            this(3);
        }
    }

}

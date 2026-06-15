package net.exylia.exyliaTotems.configs;

import net.exylia.commons.v2.config.schema.ConfigSchema;
import net.exylia.commons.v2.config.schema.ConfigSection;
import net.exylia.commons.v2.config.schema.ConfigValue;
import org.bukkit.configuration.file.FileConfiguration;

@ConfigSchema(file = "config")
public class MainConfig  {

    @ConfigSection("totem")
    public static class Totem {
        @ConfigValue("delay-ticks")
        public static int DELAY_TICKS = 3;
    }
}
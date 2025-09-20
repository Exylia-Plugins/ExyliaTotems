package net.exylia.exyliaTotems.configs;

import net.exylia.commons.config.ConfigFile;
import net.exylia.commons.config.ConfigManager;
import net.exylia.commons.config.ConfigValue;
import net.exylia.commons.config.base.MainConfigBase;
import org.bukkit.configuration.file.FileConfiguration;

@ConfigFile(value = "config", required = true)
public class MainConfig extends MainConfigBase {
    public static FileConfiguration file() {
        return ConfigManager.getFile(MainConfig.class);
    }

    @ConfigValue("totems.delay-ticks")
    private int totemsDelayTicks;

    public static int totemsDelayTicks() {
        return ConfigManager.get(MainConfig.class).totemsDelayTicks;
    }
}
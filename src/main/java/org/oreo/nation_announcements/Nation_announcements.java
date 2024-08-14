package org.oreo.nation_announcements;

import org.bukkit.plugin.java.JavaPlugin;
import org.oreo.nation_announcements.commands.AnnounceCommand;

public final class Nation_announcements extends JavaPlugin {

    @Override
    public void onEnable() {

        getLogger().info("Nation announcements enabled");

        getCommand("nation-announcements").setExecutor(new AnnounceCommand(this,this));

        saveDefaultConfig();

    }
}

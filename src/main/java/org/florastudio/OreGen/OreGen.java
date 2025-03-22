package org.florastudio.OreGen;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.florastudio.OreGen.event.OreCreateEvent;

public final class OreGen extends JavaPlugin {
    public static OreGen plugin;

    public static OreGen getInstance() {
        return plugin;
    }

    @Override
    public void onEnable() {
        plugin = this;
        long startTime = System.currentTimeMillis();
        getServer().getPluginManager().registerEvents(new OreCreateEvent(), this);
        saveDefaultConfig();

        Plugin itemAdder = Bukkit.getPluginManager().getPlugin("ItemsAdder");
        if (itemAdder == null || !itemAdder.isEnabled()) {
            getLogger().warning("ItemsAdder 플러그인과 연결되지 않았습니다.");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }
        getLogger().info("ItemsAdder 플러그인과 성공적으로 연결되었습니다!");

        if (getServer().getPluginManager().isPluginEnabled(this)) {
            getLogger().info("OreGen 플러그인 로드완료. 제작자:hunseong 로드시간: " + (System.currentTimeMillis() - startTime) + "ms");
        }
    }

    @Override
    public void onDisable() {

    }
}

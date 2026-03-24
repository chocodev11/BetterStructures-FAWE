package com.magmaguy.betterstructures.listeners;

import com.magmaguy.betterstructures.MetadataHandler;
import com.magmaguy.betterstructures.config.DefaultConfig;
import com.magmaguy.magmacore.util.Logger;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.scheduler.BukkitRunnable;

public class FirstTimeSetupWarner implements Listener {
    @EventHandler
    public void onPlayerLogin(PlayerJoinEvent event) {
        if (DefaultConfig.isSetupDone()) return;
        if (!event.getPlayer().hasPermission("betterstructures.*")) return;
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!event.getPlayer().isOnline()) return;
                Logger.sendSimpleMessage(event.getPlayer(), "&8&m----------------------------------------------------");
                Logger.sendMessage(event.getPlayer(), "&fThông điệp thiết lập ban đầu:");
                Logger.sendSimpleMessage(event.getPlayer(), "&7Chào mừng bạn đến với BetterStructures! &c&lCó vẻ như bạn chưa thiết lập BetterStructures! &2Để cài đặt BetterStructures, vui lòng chạy lệnh &a/betterstructures initialize&2!");
                Logger.sendSimpleMessage(event.getPlayer(), "&7Bạn có thể nhận hỗ trợ tại đây &9&nhttps://discord.gg/9f5QSka");
                Logger.sendSimpleMessage(event.getPlayer(), "&cChọn một tùy chọn trong /betterstructures setup để tắt vĩnh viễn thông báo này!");
                Logger.sendSimpleMessage(event.getPlayer(), "&8&m----------------------------------------------------");
            }
        }.runTaskLater(MetadataHandler.PLUGIN, 20 * 10);
    }
}

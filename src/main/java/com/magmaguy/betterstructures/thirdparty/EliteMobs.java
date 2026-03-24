package com.magmaguy.betterstructures.thirdparty;

import com.magmaguy.elitemobs.commands.ReloadCommand;
import com.magmaguy.elitemobs.mobconstructor.custombosses.RegionalBossEntity;
import com.magmaguy.magmacore.util.Logger;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class EliteMobs {
    /**
     * Spawns a 1-time regional boss at the set location
     *
     * @param location Location where the boss should spawn
     * @param filename Filename of the boss, as set in the EliteMobs custombosses configuration folder
     */
    public static boolean Spawn(Location location, String filename) {
        return spawnAndReturn(location, filename) != null;
    }

    /**
     * Spawns a 1-time regional boss at the set location and returns the spawned entity.
     *
     * @param location Location where the boss should spawn
     * @param filename Filename of the boss, as set in the EliteMobs custombosses configuration folder
     * @return The spawned Entity, or null if spawn failed
     */
    public static Entity spawnAndReturn(Location location, String filename) {
        if (Bukkit.getPluginManager().getPlugin("EliteMobs") != null) {
            RegionalBossEntity regionalBossEntity = RegionalBossEntity.SpawnRegionalBoss(filename, location);
            if (regionalBossEntity == null) {
                Logger.warn("Tạo Boss khu vực thất bại " + filename + "! Tên tệp của Boss này có thể không khớp với tên tệp trong ~/plugins/EliteMobs/custombosses/");
                return null;
            } else {
                regionalBossEntity.spawn(false);
                LivingEntity livingEntity = regionalBossEntity.getLivingEntity();
                return livingEntity;
            }
        } else {
            for (Player player : Bukkit.getOnlinePlayers())
                if (player.hasPermission("betterstructures.*"))
                    Logger.sendMessage(player, "&cMột trong các gói nội dung của bạn sử dụng plugin EliteMobs, &4nhưng EliteMobs hiện chưa được cài đặt trên máy chủ của bạn&c!" +
                            " &2Bạn có thể tải xuống tại đây: &9https://nightbreak.io/plugin/elitemobs/");
            return null;
        }
    }

    public static void Reload() {
        ReloadCommand.reload(Bukkit.getConsoleSender());
    }
}

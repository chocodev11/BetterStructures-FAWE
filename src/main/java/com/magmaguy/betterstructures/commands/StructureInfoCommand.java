package com.magmaguy.betterstructures.commands;

import com.magmaguy.betterstructures.structurelocation.StructureLocationData;
import com.magmaguy.betterstructures.structurelocation.StructureLocationManager;
import com.magmaguy.magmacore.command.AdvancedCommand;
import com.magmaguy.magmacore.command.CommandData;
import com.magmaguy.magmacore.command.SenderType;
import com.magmaguy.magmacore.util.Logger;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.List;

public class StructureInfoCommand extends AdvancedCommand {
    public StructureInfoCommand() {
        super(List.of("info"));
        setUsage("/bs info");
        setPermission("betterstructures.*");
        setDescription("Hiển thị thông tin chi tiết về công trình bạn đang ở hiện tại.");
        setSenderType(SenderType.PLAYER);
    }

    @Override
    public void execute(CommandData commandData) {
        Player player = commandData.getPlayerSender();
        Location playerLocation = player.getLocation();
        String worldName = playerLocation.getWorld().getName();

        Collection<StructureLocationData> structures =
                StructureLocationManager.getInstance().getStructuresInWorld(worldName);

        StructureLocationData found = null;
        for (StructureLocationData data : structures) {
            if (data.isWithinBounds(playerLocation)) {
                found = data;
                break;
            }
        }

        if (found == null) {
            Logger.sendMessage(player, "&cBạn hiện không ở trong phạm vi của bất kỳ công trình nào được ghi nhận.");
            return;
        }

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        Logger.sendMessage(player, "&a&l===== Thông tin công trình =====");
        Logger.sendMessage(player, "&6Bản mẫu công trình (schematic): &f" + found.schematicName());
        Logger.sendMessage(player, "&6Loại công trình: &f" + found.structureType().name());
        Logger.sendMessage(player, "&6Thế giới: &f" + found.getWorldName());
        Logger.sendMessage(player, "&6Tọa độ: &f" + found.getFormattedCoordinates());
        Logger.sendMessage(player, "&6Phạm vi: &fX=" + found.getRadiusX()
                + " Y=" + found.getRadiusY()
                + " Z=" + found.getRadiusZ());
        Logger.sendMessage(player, "&6Đã dọn dẹp: &f" + (found.isCleared() ? "có" : "không"));
        if (found.isCleared() && found.getClearedTimestamp() > 0) {
            Logger.sendMessage(player, "&6Thời gian dọn dẹp: &f" + sdf.format(new Date(found.getClearedTimestamp())));
        }
        Logger.sendMessage(player, "&6Số lần hồi sinh: &f" + found.getRespawnCount());
        Logger.sendMessage(player, "&6Công trình Boss: &f" + (found.isBossStructure() ? "có" : "không"));
        Logger.sendMessage(player, "&6Số lượng quái vật được cấu hình: &f" + found.getTotalMobCount());
        Logger.sendMessage(player, "&6Số lượng quái vật còn sống: &f" + found.getAliveMobCount());
        Logger.sendMessage(player, "&6Thời gian tạo: &f" + sdf.format(new Date(found.getCreatedTimestamp())));
    }
}

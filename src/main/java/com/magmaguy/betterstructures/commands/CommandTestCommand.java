package com.magmaguy.betterstructures.commands;

import com.magmaguy.betterstructures.mobtracking.MobTrackingManager;
import com.magmaguy.magmacore.command.AdvancedCommand;
import com.magmaguy.magmacore.command.CommandData;
import com.magmaguy.magmacore.command.SenderType;
import com.magmaguy.magmacore.util.Logger;
import org.bukkit.entity.Player;

import java.util.List;

public class CommandTestCommand extends AdvancedCommand {
    public CommandTestCommand() {
        super(List.of("commandtest"));
        setPermission("betterstructures.*");
        setUsage("/betterstructures commandtest");
        setDescription("Thử nghiệm các lệnh dọn dẹp công trình tại vị trí hiện tại của bạn.");
        setSenderType(SenderType.PLAYER);
    }

    @Override
    public void execute(CommandData commandData) {
        Player player = (Player) commandData.getCommandSender();
        Logger.sendMessage(player, "&aĐang thử nghiệm lệnh dọn dẹp công trình tại vị trí của bạn...");
        MobTrackingManager.getInstance().testCommands(player);
        Logger.sendMessage(player, "&aThử nghiệm hoàn tất! Vui lòng kiểm tra bảng điều khiển (console) để biết thêm chi tiết.");
    }
}

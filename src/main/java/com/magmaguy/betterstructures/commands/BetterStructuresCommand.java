package com.magmaguy.betterstructures.commands;

import com.magmaguy.magmacore.command.AdvancedCommand;
import com.magmaguy.magmacore.command.CommandData;
import com.magmaguy.magmacore.command.SenderType;
import com.magmaguy.magmacore.util.Logger;

import java.util.ArrayList;

public class BetterStructuresCommand extends AdvancedCommand {
    public BetterStructuresCommand() {
        super(new ArrayList<>());
        setUsage("/bs");
        setPermission("betterstructures.*");
        setDescription("Một lệnh trợ giúp cơ bản cho BetterStructures.");
        setSenderType(SenderType.PLAYER);
    }

    @Override
    public void execute(CommandData commandData) {
        Logger.sendMessage(commandData.getCommandSender(), "BetterStructures là một plugin thêm các công trình ngẫu nhiên vào thế giới Minecraft của bạn!");
        Logger.sendMessage(commandData.getCommandSender(), "Bạn có thể dùng lệnh &2/betterstructures setup &fđể xem các công trình đã có và tải xuống công trình mới.");
        Logger.sendMessage(commandData.getCommandSender(), "Sau khi cài đặt gói nội dung, các công trình sẽ tự động tạo ra trong các chunk mới mà không cần lệnh nào.");
        Logger.sendMessage(commandData.getCommandSender(), "Theo mặc định, quản trị viên sẽ nhận được thông báo khi có công trình mới được tạo ra, cho đến khi họ tắt các tin nhắn này.");
    }
}

package com.magmaguy.betterstructures.commands;

import com.magmaguy.betterstructures.config.DefaultConfig;
import com.magmaguy.magmacore.command.AdvancedCommand;
import com.magmaguy.magmacore.command.CommandData;
import com.magmaguy.magmacore.util.Logger;

import java.util.List;

public class SilentCommand extends AdvancedCommand {
    public SilentCommand() {
        super(List.of("silent"));
        setUsage("/betterstructures silent");
        setDescription("Tắt các cảnh báo về các công trình mới tạo cho quản trị viên.");
    }

    @Override
    public void execute(CommandData commandData) {
        DefaultConfig.toggleWarnings();
        Logger.sendMessage(commandData.getCommandSender(), "&2Cảnh báo tạo công trình đã được chuyển thành " + DefaultConfig.isNewBuildingWarn() + "!");
    }
}

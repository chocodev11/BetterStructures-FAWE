package com.magmaguy.betterstructures.commands;

import com.magmaguy.betterstructures.MetadataHandler;
import com.magmaguy.magmacore.command.AdvancedCommand;
import com.magmaguy.magmacore.command.CommandData;
import com.magmaguy.magmacore.util.Logger;

import java.util.List;

public class ReloadCommand extends AdvancedCommand {
    public ReloadCommand() {
        super(List.of("reload"));
        setPermission("betterstructures.*");
        setUsage("/betterstructures reload");
        setDescription("Tải lại plugin.");
    }

    @Override
    public void execute(CommandData commandData) {
        MetadataHandler.PLUGIN.onDisable();
        MetadataHandler.PLUGIN.onLoad();
        MetadataHandler.PLUGIN.onEnable();
        Logger.sendMessage(commandData.getCommandSender(), "Đã thử tải lại. Có thể không hoàn toàn áp dụng, nếu có vấn đề vui lòng khởi động lại máy chủ!");
    }
}

package com.magmaguy.betterstructures.commands;

import com.magmaguy.betterstructures.config.DefaultConfig;
import com.magmaguy.magmacore.command.AdvancedCommand;
import com.magmaguy.magmacore.command.CommandData;
import com.magmaguy.magmacore.util.Logger;

import java.util.List;

public class DebugCommand extends AdvancedCommand {
    public DebugCommand() {
        super(List.of("debug"));
        setPermission("betterstructures.*");
        setUsage("/betterstructures debug");
        setDescription("Bật/tắt các tin nhắn gỡ lỗi (debug) dành cho nhà phát triển của BetterStructures.");
    }

    @Override
    public void execute(CommandData commandData) {
        boolean enabled = DefaultConfig.toggleDeveloperMessages();
        Logger.sendMessage(commandData.getCommandSender(),
                "&2Tin nhắn dành cho nhà phát triển đã được " + (enabled ? "&abật" : "&ctắt") + "&2, không cần tải lại.");
    }
}

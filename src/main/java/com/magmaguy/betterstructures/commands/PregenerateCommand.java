package com.magmaguy.betterstructures.commands;

import com.magmaguy.betterstructures.util.ChunkPregenerator;
import com.magmaguy.magmacore.command.AdvancedCommand;
import com.magmaguy.magmacore.command.CommandData;
import com.magmaguy.magmacore.command.SenderType;
import com.magmaguy.magmacore.command.arguments.IntegerCommandArgument;
import com.magmaguy.magmacore.command.arguments.ListStringCommandArgument;
import com.magmaguy.magmacore.util.Logger;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.List;

public class PregenerateCommand extends AdvancedCommand {
    public PregenerateCommand() {
        super(List.of("pregenerate"));
        addArgument("center", new ListStringCommandArgument(List.of("HERE", "WORLD_CENTER", "WORLD_SPAWN"), "Center of the generation"));
        addArgument("shape", new ListStringCommandArgument(List.of("SQUARE", "CIRCLE"), "Shape of the generation"));
        addArgument("radius", new IntegerCommandArgument("Radius in blocks to generate"));
        addArgument("setWorldBorder", new ListStringCommandArgument(List.of("TRUE", "FALSE"), "Set a world border at the end?"));
        setUsage("/betterstructures pregenerate <centerType> <shape> <radiusInBlocks> <applyWorldBorder>");
        setPermission("betterstructures.*");
        setDescription("Tạo trước các chunk từ một điểm trung tâm hướng ra ngoài theo hình vuông hoặc hình tròn cho đến bán kính khối chỉ định.");
        setSenderType(SenderType.PLAYER);
    }

    @Override
    public void execute(CommandData commandData) {
        String centerArg = commandData.getStringArgument("center");
        String shape = commandData.getStringArgument("shape");
        int radius = commandData.getIntegerArgument("radius");
        String setWorldBorderArg = commandData.getStringArgument("setWorldBorder");

        if (radius < 0) {
            Logger.sendMessage(commandData.getCommandSender(), "&cBán kính phải lớn hơn hoặc bằng 0.");
            return;
        }

        World world = commandData.getPlayerSender().getWorld();
        Location center;

        // Determine center location based on argument
        switch (centerArg.toUpperCase()) {
            case "HERE":
                center = commandData.getPlayerSender().getLocation();
                break;
            case "WORLD_CENTER":
                center = new Location(world, 0, world.getHighestBlockYAt(0, 0), 0);
                break;
            case "WORLD_SPAWN":
                center = world.getSpawnLocation();
                break;
            default:
                Logger.sendMessage(commandData.getCommandSender(), "&cTham số điểm trung tâm không hợp lệ. Vui lòng sử dụng HERE, WORLD_CENTER, hoặc WORLD_SPAWN.");
                return;
        }

        boolean setWorldBorder = "TRUE".equalsIgnoreCase(setWorldBorderArg);

        if (!"SQUARE".equalsIgnoreCase(shape) && !"CIRCLE".equalsIgnoreCase(shape)) {
            Logger.sendMessage(commandData.getCommandSender(), "&cHình dạng không hợp lệ. Vui lòng sử dụng SQUARE hoặc CIRCLE.");
            return;
        }

        int radiusInBlocks = radius;
        int radiusInChunks = (int) Math.ceil(radiusInBlocks / 16.0);

        Logger.sendMessage(commandData.getCommandSender(), "&2Bắt đầu tạo trước chunk, hình dạng: " + shape + ", center: " + centerArg + ", radius: " + radiusInBlocks + " blocks (" + radiusInChunks + " chunks)");
        if (setWorldBorder) {
            Logger.sendMessage(commandData.getCommandSender(), "&2Biên giới thế giới sẽ được đặt để khớp với khu vực tạo ra.");
        }
        Logger.sendMessage(commandData.getCommandSender(), "&7Tiến độ sẽ được báo cáo trên bảng điều khiển (console) mỗi 30 giây.");
        Logger.sendMessage(commandData.getCommandSender(), "&7Nếu cần hủy, vui lòng sử dụng lệnh &2/betterstructures cancelPregenerate &7.");

        ChunkPregenerator pregenerator = new ChunkPregenerator(world, center, shape, radiusInBlocks, radiusInChunks, setWorldBorder);
        pregenerator.start();
    }
}


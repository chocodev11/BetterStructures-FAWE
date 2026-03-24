package com.magmaguy.betterstructures.commands;

import com.magmaguy.betterstructures.config.treasures.TreasureConfig;
import com.magmaguy.betterstructures.config.treasures.TreasureConfigFields;
import com.magmaguy.betterstructures.util.ItemStackSerialization;
import com.magmaguy.magmacore.command.AdvancedCommand;
import com.magmaguy.magmacore.command.CommandData;
import com.magmaguy.magmacore.command.arguments.IntegerCommandArgument;
import com.magmaguy.magmacore.command.arguments.ListStringCommandArgument;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LootifyCommand extends AdvancedCommand {
    public LootifyCommand() {
        super(List.of("lootify"));
        ArrayList<String> treasures = new ArrayList<>(TreasureConfig.getTreasureConfigurations().keySet());
        addArgument("generator", new ListStringCommandArgument(treasures,"<treasures>"));
        addArgument("rarity", new ListStringCommandArgument("<rarity>"));
        addArgument("minAmount", new IntegerCommandArgument("<minAmount>"));
        addArgument("maxAmount", new IntegerCommandArgument("<maxAmount>"));
        addArgument("weight", new IntegerCommandArgument("<weight>"));
        setPermission("betterstructures.*");
        setUsage("/betterstructures lootify <generator> <rarity> <minAmount> <maxAmount> <weight>");
        setDescription("Thêm vật phẩm đang cầm vào cài đặt phần thưởng (loot) của một máy tạo");
    }

    @Override
    public void execute(CommandData commandData) {
        lootify(commandData.getStringArgument("generator"),
                commandData.getStringArgument("rarity"),
                commandData.getStringArgument("minAmount"),
                commandData.getStringArgument("maxAmount"),
                commandData.getStringArgument("weight"),
                commandData.getPlayerSender());
    }
    private void lootify(String generator, String rarity, String minAmount, String maxAmount, String weight, Player player) {
        TreasureConfigFields treasureConfigFields = TreasureConfig.getConfigFields(generator);
        if (treasureConfigFields == null) {
            player.sendMessage("[BetterStructures] Máy tạo không hợp lệ! Vui lòng thử lại.");
            return;
        }
        //Verify loot table
        if (treasureConfigFields.getRawLoot().get(rarity) == null) {
            player.sendMessage("[BetterStructures] Độ hiếm không hợp lệ! Vui lòng thử lại.");
            return;
        }
        int minAmountInt;
        try {
            minAmountInt = Integer.parseInt(minAmount);
        } catch (Exception exception) {
            player.sendMessage("[BetterStructures] Số lượng tối thiểu không hợp lệ! Vui lòng thử lại.");
            return;
        }
        if (minAmountInt < 1) {
            player.sendMessage("[BetterStructures] Số lượng tối thiểu không được nhỏ hơn 1! Giá trị này sẽ không được lưu.");
            return;
        }
        int maxAmountInt;
        try {
            maxAmountInt = Integer.parseInt(maxAmount);
        } catch (Exception exception) {
            player.sendMessage("[BetterStructures] Số lượng tối đa không hợp lệ! Vui lòng thử lại.");
            return;
        }
        if (maxAmountInt > 64) {
            player.sendMessage("[BetterStructures] Số lượng tối đa không được vượt quá 64! Nếu cần nhiều hơn, vui lòng tạo nhiều mục lục. Giá trị này sẽ không được lưu.");
            return;
        }
        double weightDouble;
        try {
            weightDouble = Double.parseDouble(weight);
        } catch (Exception exception) {
            player.sendMessage("[BetterStructures] Trọng số không hợp lệ! Vui lòng thử lại.");
            return;
        }
        ItemStack itemStack = player.getInventory().getItemInMainHand();
        if (itemStack == null || itemStack.getType().isAir()) {
            player.sendMessage("[BetterStructures] Bạn cần phải cầm một vật phẩm trên tay để đăng ký! Giá trị này sẽ không được lưu.");
            return;
        }
        String info;
        if (itemStack.hasItemMeta() && itemStack.getItemMeta().hasDisplayName())
            info = itemStack.getItemMeta().getDisplayName().replace(" ", "_");
        else if (itemStack.hasItemMeta() && itemStack.getItemMeta().hasItemName())
            info = itemStack.getItemMeta().getItemName();
        else
            info = itemStack.getType().toString();
        Map<String, Object> configMap = new HashMap<>();
        configMap.put("serialized", ItemStackSerialization.deserializeItem(itemStack));
        configMap.put("amount", minAmount +"-"+maxAmount);
        configMap.put("weight", weightDouble);
        configMap.put("info", info);
        treasureConfigFields.addChestEntry(configMap, rarity, player);
    }
}

package com.magmaguy.betterstructures.content;

import com.magmaguy.betterstructures.MetadataHandler;
import com.magmaguy.betterstructures.config.contentpackages.ContentPackageConfigFields;
import com.magmaguy.betterstructures.config.schematics.SchematicConfig;
import com.magmaguy.betterstructures.config.schematics.SchematicConfigField;
import com.magmaguy.magmacore.menus.ContentPackage;
import com.magmaguy.magmacore.util.ItemStackGenerator;
import com.magmaguy.magmacore.util.Logger;
import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BSPackage extends ContentPackage {

    @Getter
    private static final Map<String, BSPackage> bsPackages = new HashMap<>();
    private static final String schematicFolder = MetadataHandler.PLUGIN.getDataFolder().getAbsolutePath() + File.separatorChar + "schematics" + File.separatorChar;
    private static final String modulesFolder = MetadataHandler.PLUGIN.getDataFolder().getAbsolutePath() + File.separatorChar + "modules" + File.separatorChar;
    @Getter
    private final ContentPackageConfigFields contentPackageConfigFields;

    public BSPackage(ContentPackageConfigFields contentPackageConfigFields) {
        super();
        this.contentPackageConfigFields = contentPackageConfigFields;
        bsPackages.put(contentPackageConfigFields.getFilename(), this);
    }

    public static void shutdown() {
        bsPackages.clear();
    }

    @Override
    protected void doInstall(Player player) {
        player.closeInventory();
        File folder = getSpecificSchematicFolder();
        if (!folder.exists()) {
            Logger.sendMessage(player, "Không tìm thấy thư mục " + folder.getAbsolutePath());
            return;
        }

        for (File file : folder.listFiles()) {
            if (!file.getName().endsWith(".yml")) continue;
            SchematicConfigField schematicConfigField = SchematicConfig.getSchematicConfiguration(file.getName());
            schematicConfigField.toggleEnabled(true);
        }

        contentPackageConfigFields.setEnabledAndSave(true);

        MetadataHandler.PLUGIN.onDisable();
        MetadataHandler.PLUGIN.onLoad();
        MetadataHandler.PLUGIN.onEnable();
        Logger.sendMessage(player, " Đã thử tải lại. Có thể không hoàn toàn có hiệu lực, nếu có vấn đề vui lòng khởi động lại máy chủ!");
        Logger.sendMessage(player, "Đã cài đặt " + contentPackageConfigFields.getName());
    }

    @Override
    public void doUninstall(Player player) {
        player.closeInventory();
        File folder = getSpecificSchematicFolder();
        if (!folder.exists()) {
            Logger.sendMessage(player, "Không tìm thấy thư mục " + folder.getAbsolutePath());
            return;
        }

        for (File file : folder.listFiles()) {
            if (!file.getName().endsWith(".yml")) continue;
            SchematicConfigField schematicConfigField = SchematicConfig.getSchematicConfiguration(file.getName());
            schematicConfigField.toggleEnabled(false);
        }

        contentPackageConfigFields.setEnabledAndSave(false);

        MetadataHandler.PLUGIN.onDisable();
        MetadataHandler.PLUGIN.onLoad();
        MetadataHandler.PLUGIN.onEnable();
        Logger.sendMessage(player, " Đã thử tải lại. Có thể không hoàn toàn có hiệu lực, nếu có vấn đề vui lòng khởi động lại máy chủ!");

        Logger.sendMessage(player, "Đã gỡ cài đặt " + contentPackageConfigFields.getName());
    }

    @Override
    public void doDownload(Player player) {
        player.closeInventory();
        player.sendMessage("----------------------------------------------------");
        Logger.sendMessage(player, "&4Vui lòng tải xuống tại đây &9 " + contentPackageConfigFields.getDownloadLink());
        player.sendMessage("----------------------------------------------------");
    }

    @Override
    protected ItemStack getInstalledItemStack() {
        List<String> lore = new ArrayList<>(contentPackageConfigFields.getDescription());
        lore.addAll(List.of("Đã cài đặt nội dung!", "Nhấp để gỡ cài đặt!"));
        return ItemStackGenerator.generateItemStack(Material.GREEN_STAINED_GLASS_PANE, contentPackageConfigFields.getName(), lore);
    }

    @Override
    protected ItemStack getPartiallyInstalledItemStack() {
        List<String> lore = new ArrayList<>(contentPackageConfigFields.getDescription());
        lore.addAll(List.of(
                "Nội dung được cài đặt một phần!",
                "Nguyên nhân có thể là do bạn chưa tải xuống hoàn tất,",
                "hoặc một số yếu tố đã bị tắt thủ công.",
                "Nhấp để tải xuống!"));
        return ItemStackGenerator.generateItemStack(Material.ORANGE_STAINED_GLASS_PANE, contentPackageConfigFields.getName(), lore);
    }

    @Override
    protected ItemStack getNotInstalledItemStack() {
        List<String> lore = new ArrayList<>(contentPackageConfigFields.getDescription());
        lore.addAll(List.of("Nội dung chưa được cài đặt!", "Nhấp để cài đặt!"));
        return ItemStackGenerator.generateItemStack(Material.YELLOW_STAINED_GLASS_PANE, contentPackageConfigFields.getName(), lore);
    }

    @Override
    protected ItemStack getNotDownloadedItemStack() {
        List<String> lore = new ArrayList<>(contentPackageConfigFields.getDescription());
        lore.addAll(List.of("Nội dung chưa được tải xuống!", "Nhấp để lấy liên kết tải xuống!"));
        return ItemStackGenerator.generateItemStack(Material.RED_STAINED_GLASS_PANE, contentPackageConfigFields.getName(), lore);
    }

    @Override
    protected ContentState getContentState() {
        if (!isInstalled()) return ContentState.NOT_DOWNLOADED;
        if (contentPackageConfigFields.isEnabled()) return ContentState.INSTALLED;
        return ContentState.NOT_INSTALLED;
    }

    private File getSpecificSchematicFolder() {
        return new File(schematicFolder + contentPackageConfigFields.getFolderName());
    }

    private boolean isInstalled() {
        if (contentPackageConfigFields.getContentPackageType().equals(ContentPackageConfigFields.ContentPackageType.MODULAR)){
            return new File(modulesFolder + contentPackageConfigFields.getFolderName()).exists();
        } else {
            return new File(schematicFolder + contentPackageConfigFields.getFolderName()).exists();
        }
    }
}
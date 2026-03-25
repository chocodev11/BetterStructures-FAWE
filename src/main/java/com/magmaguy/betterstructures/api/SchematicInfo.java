package com.magmaguy.betterstructures.api;

import com.magmaguy.betterstructures.schematics.SchematicContainer;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.world.block.BlockState;
import org.bukkit.Material;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Metadata snapshot for a loaded BetterStructures schematic.
 */
public record SchematicInfo(
        String name,
        int width,
        int height,
        int depth,
        boolean requiresChestFill,
        boolean hasEntities,
        Set<Material> requiredMaterials
) {
    public SchematicInfo {
        requiredMaterials = requiredMaterials == null ? Set.of() : Set.copyOf(requiredMaterials);
    }

    public static SchematicInfo fromSchematic(SchematicContainer container) {
        LinkedHashSet<Material> materials = new LinkedHashSet<>();

        container.getClipboard().getRegion().forEach(blockPos -> {
            BlockState blockState = container.getClipboard().getBlock(blockPos);
            Material material = BukkitAdapter.adapt(blockState.getBlockType());
            if (material == null || material.isAir() || material == Material.BARRIER) {
                return;
            }
            materials.add(material);
        });

        boolean requiresChestFill = !container.getChestLocations().isEmpty()
                && (container.getChestContents() != null
                || container.getGeneratorConfigFields().getChestContents() != null);
        boolean hasEntities = !container.getVanillaSpawns().isEmpty()
                || !container.getEliteMobsSpawns().isEmpty()
                || !container.getMythicMobsSpawns().isEmpty();

        return new SchematicInfo(
                stripExtension(container.getClipboardFilename()),
                container.getClipboard().getDimensions().x(),
                container.getClipboard().getDimensions().y(),
                container.getClipboard().getDimensions().z(),
                requiresChestFill,
                hasEntities,
                materials
        );
    }

    private static String stripExtension(String schematicName) {
        if (schematicName == null) {
            return "";
        }
        if (schematicName.endsWith(".schem")) {
            return schematicName.substring(0, schematicName.length() - 6);
        }
        if (schematicName.endsWith(".yml")) {
            return schematicName.substring(0, schematicName.length() - 4);
        }
        return schematicName;
    }
}

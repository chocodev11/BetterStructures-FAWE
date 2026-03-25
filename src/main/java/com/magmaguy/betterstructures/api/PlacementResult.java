package com.magmaguy.betterstructures.api;

import com.magmaguy.betterstructures.structurelocation.StructureLocationData;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;

import java.util.Optional;

/**
 * Result for a BetterStructures API placement attempt.
 */
public record PlacementResult(
        boolean success,
        String schematicName,
        Location location,
        BlockFace rotation,
        boolean mirror,
        Optional<StructureLocationData> structureData,
        Optional<String> errorMessage,
        long placementTimeMs
) {
    public PlacementResult {
        structureData = structureData == null ? Optional.empty() : structureData;
        errorMessage = errorMessage == null ? Optional.empty() : errorMessage;
        location = cloneLocation(location);
    }

    public static PlacementResult success(
            String schematicName,
            Location location,
            BlockFace rotation,
            boolean mirror,
            StructureLocationData structureData,
            long placementTimeMs
    ) {
        return new PlacementResult(
                true,
                schematicName,
                location,
                rotation,
                mirror,
                Optional.ofNullable(structureData),
                Optional.empty(),
                placementTimeMs
        );
    }

    public static PlacementResult failure(
            String schematicName,
            Location location,
            BlockFace rotation,
            boolean mirror,
            String errorMessage,
            long placementTimeMs
    ) {
        return new PlacementResult(
                false,
                schematicName,
                location,
                rotation,
                mirror,
                Optional.empty(),
                Optional.ofNullable(errorMessage),
                placementTimeMs
        );
    }

    private static Location cloneLocation(Location location) {
        return location == null ? null : location.clone();
    }
}

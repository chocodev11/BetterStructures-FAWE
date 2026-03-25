package com.magmaguy.betterstructures.api;

import org.bukkit.block.BlockFace;

/**
 * Immutable placement configuration for BetterStructures API calls.
 */
public final class PlacementOptions {
    private final BlockFace rotation;
    private final boolean mirror;
    private final boolean fillChests;
    private final boolean spawnEntities;
    private final boolean bypassSpawnProtection;
    private final boolean recordInManager;
    private final String generatorId;

    private PlacementOptions(Builder builder) {
        this.rotation = validateRotation(builder.rotation);
        this.mirror = builder.mirror;
        this.fillChests = builder.fillChests;
        this.spawnEntities = builder.spawnEntities;
        this.bypassSpawnProtection = builder.bypassSpawnProtection;
        this.recordInManager = builder.recordInManager;
        this.generatorId = builder.generatorId == null || builder.generatorId.isBlank()
                ? "api_placement"
                : builder.generatorId;
    }

    public static Builder builder(BlockFace rotation) {
        return new Builder(rotation);
    }

    public BlockFace getRotation() {
        return rotation;
    }

    public boolean isMirror() {
        return mirror;
    }

    public boolean isFillChests() {
        return fillChests;
    }

    public boolean isSpawnEntities() {
        return spawnEntities;
    }

    public boolean isBypassSpawnProtection() {
        return bypassSpawnProtection;
    }

    public boolean isRecordInManager() {
        return recordInManager;
    }

    public String getGeneratorId() {
        return generatorId;
    }

    private static BlockFace validateRotation(BlockFace rotation) {
        if (rotation == null) {
            throw new IllegalArgumentException("Rotation cannot be null");
        }

        if (rotation != BlockFace.NORTH
                && rotation != BlockFace.SOUTH
                && rotation != BlockFace.EAST
                && rotation != BlockFace.WEST) {
            throw new IllegalArgumentException("Rotation must be NORTH, SOUTH, EAST, or WEST");
        }

        return rotation;
    }

    public static final class Builder {
        private final BlockFace rotation;
        private boolean mirror = false;
        private boolean fillChests = true;
        private boolean spawnEntities = true;
        private boolean bypassSpawnProtection = false;
        private boolean recordInManager = true;
        private String generatorId = "api_placement";

        public Builder(BlockFace rotation) {
            this.rotation = rotation;
        }

        public Builder mirror(boolean mirror) {
            this.mirror = mirror;
            return this;
        }

        public Builder fillChests(boolean fillChests) {
            this.fillChests = fillChests;
            return this;
        }

        public Builder spawnEntities(boolean spawnEntities) {
            this.spawnEntities = spawnEntities;
            return this;
        }

        public Builder bypassSpawnProtection(boolean bypassSpawnProtection) {
            this.bypassSpawnProtection = bypassSpawnProtection;
            return this;
        }

        public Builder recordInManager(boolean recordInManager) {
            this.recordInManager = recordInManager;
            return this;
        }

        public Builder generatorId(String generatorId) {
            this.generatorId = generatorId;
            return this;
        }

        public PlacementOptions build() {
            return new PlacementOptions(this);
        }
    }
}

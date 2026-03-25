package com.magmaguy.betterstructures.buildingfitter;

import com.magmaguy.betterstructures.MetadataHandler;
import com.magmaguy.betterstructures.api.BuildPlaceEvent;
import com.magmaguy.betterstructures.api.ChestFillEvent;
import com.magmaguy.betterstructures.api.PlacementOptions;
import com.magmaguy.betterstructures.api.PlacementResult;
import com.magmaguy.betterstructures.api.StructurePlacedEvent;
import com.magmaguy.betterstructures.buildingfitter.util.FitUndergroundDeepBuilding;
import com.magmaguy.betterstructures.buildingfitter.util.LocationProjector;
import com.magmaguy.betterstructures.buildingfitter.util.SchematicPicker;
import com.magmaguy.betterstructures.config.DefaultConfig;
import com.magmaguy.betterstructures.config.generators.GeneratorConfigFields;
import com.magmaguy.betterstructures.mobtracking.MobSpawnConfig;
import com.magmaguy.betterstructures.mobtracking.MobTrackingManager;
import com.magmaguy.betterstructures.schematics.SchematicContainer;
import com.magmaguy.betterstructures.structurelocation.StructureLocationData;
import com.magmaguy.betterstructures.structurelocation.StructureLocationManager;
import com.magmaguy.betterstructures.thirdparty.EliteMobs;
import com.magmaguy.betterstructures.thirdparty.MythicMobs;
import com.magmaguy.betterstructures.thirdparty.WorldGuard;
import com.magmaguy.betterstructures.util.ChunkProcessingMarker;
import com.magmaguy.betterstructures.util.DeveloperLogger;
import com.magmaguy.betterstructures.util.SurfaceMaterials;
import com.magmaguy.betterstructures.util.WorldEditUtils;
import com.magmaguy.betterstructures.worldedit.Schematic;
import com.magmaguy.magmacore.util.Logger;
import com.magmaguy.magmacore.util.SpigotMessage;
import com.magmaguy.magmacore.util.VersionChecker;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.transform.AffineTransform;
import com.sk89q.worldedit.world.block.BaseBlock;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Container;
import org.bukkit.entity.*;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;
import java.util.function.Function;

public class FitAnything {
    public static boolean worldGuardWarn = false;
    protected final int searchRadius = 1;
    protected final int scanStep = 3;
    private final HashMap<Material, Integer> undergroundPedestalMaterials = new HashMap<>();
    private final HashMap<Material, Integer> surfacePedestalMaterials = new HashMap<>();
    @Getter
    protected SchematicContainer schematicContainer;
    protected double startingScore = 100;
    @Getter
    protected Clipboard schematicClipboard = null;
    @Getter
    protected Vector schematicOffset;
    protected int verticalOffset = 0;
    //At 10% it is assumed a fit is so bad it's better just to skip
    protected double highestScore = 10;
    @Getter
    protected Location location = null;
    protected GeneratorConfigFields.StructureType structureType;
    private Material pedestalMaterial = null;
    private boolean apiPlacement = false;
    private PlacementOptions apiOptions = null;
    private Consumer<PlacementResult> apiCallback = null;
    private long apiPlacementStartTime = 0L;
    private final List<Vector> apiChestLocations = new ArrayList<>();
    private final Map<Vector, EntityType> apiVanillaSpawns = new HashMap<>();
    private final Map<Vector, String> apiEliteMobsSpawns = new HashMap<>();
    private final Map<Vector, String> apiMythicMobsSpawns = new HashMap<>();

    public FitAnything(SchematicContainer schematicContainer) {
        this.schematicContainer = schematicContainer;
        this.schematicClipboard = schematicContainer.getClipboard();
        this.schematicOffset = WorldEditUtils.getSchematicOffset(this.schematicClipboard);
        this.structureType = schematicContainer.getGeneratorConfigFields().getStructureTypes().isEmpty()
                ? GeneratorConfigFields.StructureType.UNDEFINED
                : schematicContainer.getGeneratorConfigFields().getStructureTypes().get(0);
        this.verticalOffset = schematicContainer.getClipboard().getMinimumPoint().y() - schematicContainer.getClipboard().getOrigin().y();
    }

    public FitAnything() {
    }
    
    public FitAnything(
            SchematicContainer schematicContainer,
            Location location,
            PlacementOptions options,
            Consumer<PlacementResult> completionCallback
    ) {
        this(schematicContainer);
        this.location = location;
        this.apiPlacement = true;
        this.apiOptions = options;
        this.apiCallback = completionCallback;
        this.apiPlacementStartTime = System.currentTimeMillis();
        applyApiTransformation();
        analyzeApiClipboard();
    }
    
    /**
     * Direct paste method for API calls (skips chunk-based scanning).
     * Called by BetterStructuresAPI after creating FitAnything with API constructor.
     *
     * @param location The location to paste at
     */
    public void pasteDirect(Location location) {
        this.location = location;
        paste(location, null);
    }

    public boolean isApiPlacement() {
        return apiPlacement;
    }

    public PlacementOptions getApiOptions() {
        return apiOptions;
    }

    private void applyApiTransformation() {
        if (!apiPlacement || apiOptions == null) {
            return;
        }

        AffineTransform transform = new AffineTransform();
        if (apiOptions.isMirror()) {
            transform = transform.scale(-1, 1, 1);
        }
        transform = transform.rotateY(rotationToDegrees(apiOptions.getRotation()));

        try {
            this.schematicClipboard = this.schematicClipboard.transform(transform);
            this.schematicOffset = WorldEditUtils.getSchematicOffset(this.schematicClipboard);
            this.verticalOffset = this.schematicClipboard.getMinimumPoint().y() - this.schematicClipboard.getOrigin().y();
        } catch (WorldEditException exception) {
            throw new IllegalStateException("Unable to transform schematic clipboard", exception);
        }
    }

    private void analyzeApiClipboard() {
        if (!apiPlacement) {
            return;
        }

        apiChestLocations.clear();
        apiVanillaSpawns.clear();
        apiEliteMobsSpawns.clear();
        apiMythicMobsSpawns.clear();

        for (BlockVector3 blockPos : schematicClipboard.getRegion()) {
            BaseBlock baseBlock = schematicClipboard.getFullBlock(blockPos);
            Material material = BukkitAdapter.adapt(baseBlock.getBlockType());
            if (material == null) {
                continue;
            }

            Vector relative = new Vector(
                    blockPos.x() - schematicClipboard.getMinimumPoint().x(),
                    blockPos.y() - schematicClipboard.getMinimumPoint().y(),
                    blockPos.z() - schematicClipboard.getMinimumPoint().z()
            );

            if (material == Material.CHEST
                    || material == Material.TRAPPED_CHEST
                    || material == Material.SHULKER_BOX) {
                apiChestLocations.add(relative);
            }

            if (!isTrackedSign(material)) {
                continue;
            }

            String line1 = WorldEditUtils.getLine(baseBlock, 1);
            if (line1 == null || line1.isBlank()) {
                continue;
            }

            if (line1.toLowerCase(Locale.ROOT).contains("[spawn]")) {
                String line2 = WorldEditUtils.getLine(baseBlock, 2);
                if (line2 == null || line2.isBlank()) {
                    continue;
                }
                try {
                    apiVanillaSpawns.put(relative, EntityType.valueOf(line2.toUpperCase(Locale.ROOT).replace("\"", "")));
                } catch (IllegalArgumentException ignored) {
                    if ("WITHER_CRYSTAL".equalsIgnoreCase(line2)) {
                        apiVanillaSpawns.put(relative, EntityType.END_CRYSTAL);
                    }
                }
                continue;
            }

            if (line1.toLowerCase(Locale.ROOT).contains("[elitemobs]")) {
                StringBuilder filename = new StringBuilder();
                for (int i = 2; i < 5; i++) {
                    String line = WorldEditUtils.getLine(baseBlock, i);
                    if (line != null) {
                        filename.append(line);
                    }
                }
                apiEliteMobsSpawns.put(relative, filename.toString());
                continue;
            }

            if (line1.toLowerCase(Locale.ROOT).contains("[mythicmobs]")) {
                String mob = WorldEditUtils.getLine(baseBlock, 2);
                String level = WorldEditUtils.getLine(baseBlock, 3);
                if (mob == null || mob.isBlank()) {
                    continue;
                }
                apiMythicMobsSpawns.put(relative, mob + (level == null || level.isBlank() ? "" : ":" + level));
            }
        }
    }

    private static boolean isTrackedSign(Material material) {
        String name = material.name();
        return name.endsWith("_SIGN") || name.endsWith("_WALL_SIGN") || name.endsWith("_HANGING_SIGN");
    }

    private static int rotationToDegrees(BlockFace rotation) {
        return switch (rotation) {
            case EAST -> 270;
            case SOUTH -> 180;
            case WEST -> 90;
            default -> 0;
        };
    }

    public static void commandBasedCreation(Chunk chunk, GeneratorConfigFields.StructureType structureType, SchematicContainer container) {
        switch (structureType) {
            case SKY:
                new FitAirBuilding(chunk, container);
                break;
            case SURFACE:
                new FitSurfaceBuilding(chunk, container);
                break;
            case LIQUID_SURFACE:
                new FitLiquidBuilding(chunk, container);
                break;
            case UNDERGROUND_DEEP:
                FitUndergroundDeepBuilding.fit(chunk, container);
                break;
            case UNDERGROUND_SHALLOW:
                FitUndergroundShallowBuilding.fit(chunk, container);
                break;
            default:
        }
    }

    protected void randomizeSchematicContainer(Location location, GeneratorConfigFields.StructureType structureType) {
        if (schematicClipboard != null) return;
        schematicContainer = SchematicPicker.pick(location, structureType);
        if (schematicContainer != null) {
            schematicClipboard = schematicContainer.getClipboard();
            verticalOffset = schematicContainer.getClipboard().getMinimumPoint().y() - schematicContainer.getClipboard().getOrigin().y();
        }
    }

    protected void paste(Location location) {
        paste(location, null);
    }

    protected void paste(Location location, Chunk sourceChunk) {
        // Ensure paste runs on main thread since BuildPlaceEvent must fire on main thread
        // and this method may be called from async terrain scanning
        Runnable pasteLogic = () -> {
            BuildPlaceEvent buildPlaceEvent = new BuildPlaceEvent(this);
            Bukkit.getServer().getPluginManager().callEvent(buildPlaceEvent);
            if (buildPlaceEvent.isCancelled()) {
                completeApiResult(PlacementResult.failure(
                        getResolvedSchematicName(),
                        location,
                        getPlacementRotation(),
                        isPlacementMirror(),
                        "Placement cancelled by BuildPlaceEvent",
                        getPlacementDurationMs()
                ));
                return;
            }

            FitAnything fitAnything = this;

            // Create prePasteCallback that runs AFTER chunks are ready but BEFORE paste
            // This ensures assignPedestalMaterial can safely access world blocks
            Runnable prePasteCallback = () -> {
                // Set pedestal material - now safe because chunks are generated
                assignPedestalMaterial(location);
                if (pedestalMaterial == null)
                    switch (location.getWorld().getEnvironment()) {
                        case NETHER:
                            pedestalMaterial = Material.NETHERRACK;
                            break;
                        case THE_END:
                            pedestalMaterial = Material.END_STONE;
                            break;
                        default:
                            pedestalMaterial = Material.STONE;
                    }
            };

            // Create a function to provide pedestal material
            Function<Boolean, Material> pedestalMaterialProvider = this::getPedestalMaterial;
            Consumer<Schematic.PasteResult> onPasteResult = pasteResult -> {
                if (pasteResult.success()) {
                    if (sourceChunk != null && sourceChunk.isLoaded()) {
                        ChunkProcessingMarker.markProcessed(sourceChunk);
                        DeveloperLogger.debug("PASTE_SUCCESS_MARKED: " + sourceChunk.getWorld().getName() + " "
                                + sourceChunk.getX() + "," + sourceChunk.getZ()
                                + " schematic=" + schematicContainer.getConfigFilename());
                    }
                    onPasteComplete(fitAnything, location).run();
                } else {
                    String schematicName = fitAnything.schematicContainer != null
                            ? fitAnything.schematicContainer.getConfigFilename()
                            : "unknown";
                    DeveloperLogger.debug("PASTE_FAILED: " + location.getWorld().getName() + " "
                            + location.getBlockX() + "," + location.getBlockY() + "," + location.getBlockZ()
                            + " schematic=" + schematicName
                            + " reason=" + pasteResult.reason());
                    completeApiResult(PlacementResult.failure(
                            getResolvedSchematicName(),
                            location,
                            getPlacementRotation(),
                            isPlacementMirror(),
                            formatPlacementFailure(pasteResult.reason()),
                            getPlacementDurationMs()
                    ));
                }
            };

            // Paste the schematic with chunk-safe callback
            Schematic.pasteSchematic(
                    schematicClipboard,
                    location,
                    schematicOffset,
                    prePasteCallback,
                    pedestalMaterialProvider,
                    onPasteResult
            );
        };

        // If already on main thread, run directly; otherwise schedule to main thread
        if (Bukkit.isPrimaryThread()) {
            pasteLogic.run();
        } else {
            Bukkit.getScheduler().runTask(MetadataHandler.PLUGIN, pasteLogic);
        }
    }

    private BukkitRunnable onPasteComplete(FitAnything fitAnything, Location location) {
        return new BukkitRunnable() {
            @Override
            public void run() {
                String schematicName = fitAnything.getResolvedSchematicName();
                PlacementOptions placementOptions = fitAnything.getEffectivePlacementOptions();
                long placementTimeMs = fitAnything.getPlacementDurationMs();
                
                if (DefaultConfig.isNewBuildingWarn() && fitAnything.schematicContainer != null) {
                    String structureTypeString = fitAnything.structureType.toString().toLowerCase(Locale.ROOT).replace("_", " ");
                    for (Player player : Bukkit.getOnlinePlayers())
                        if (player.hasPermission("betterstructures.warn"))
                            player.spigot().sendMessage(
                                    SpigotMessage.commandHoverMessage("[BetterStructures] Công trình " + structureTypeString + " mới đã được tạo! Nhấp để dịch chuyển. Dùng lệnh \"/betterstructures silent\" để dừng cảnh báo!",
                                            "Nhấp để dịch chuyển đến " + location.getWorld().getName() + ", " + location.getBlockX() + ", " + location.getBlockY() + ", " + location.getBlockZ() + "\n Tên mẫu: " + schematicName,
                                            "/betterstructures teleport " + location.getWorld().getName() + " " + location.getBlockX() + " " + location.getBlockY() + " " + location.getBlockZ())
                            );
                }

                StructureLocationData structureData = null;
                if (fitAnything.shouldRecordInManager()) {
                    structureData = StructureLocationManager.getInstance().recordStructure(
                            location,
                            schematicName,
                            fitAnything.structureType
                    );
                    if (structureData != null) {
                        StructurePlacedEvent placedEvent = new StructurePlacedEvent(
                                structureData,
                                placementOptions,
                                placementTimeMs
                        );
                        Bukkit.getServer().getPluginManager().callEvent(placedEvent);
                    }
                }

                if (!(fitAnything instanceof FitAirBuilding)) {
                    try {
                        addPedestal(location);
                    } catch (Exception exception) {
                        Logger.warn("Lỗi phân bổ vật liệu đế!");
                        exception.printStackTrace();
                    }
                    try {
                        if (fitAnything instanceof FitSurfaceBuilding)
                            clearTrees(location);
                    } catch (Exception exception) {
                        Logger.warn("Lỗi dọn dẹp cây cối!");
                        exception.printStackTrace();
                    }
                }
                if (fitAnything.shouldFillChests()) {
                    try {
                        fillChests();
                    } catch (Exception exception) {
                        Logger.warn("Lỗi điền vào rương!");
                        exception.printStackTrace();
                    }
                }
                if (fitAnything.shouldSpawnEntities()) {
                    try {
                        spawnEntities();
                    } catch (Exception exception) {
                        Logger.warn("Lỗi tạo thực thể!");
                        exception.printStackTrace();
                    }
                    try{
                        spawnProps(fitAnything.schematicClipboard);
                    } catch (Exception exception) {
                        Logger.warn("Lỗi tạo đồ trang trí!");
                        exception.printStackTrace();
                    }
                }

                fitAnything.completeApiResult(PlacementResult.success(
                        schematicName,
                        location,
                        placementOptions.getRotation(),
                        placementOptions.isMirror(),
                        structureData,
                        placementTimeMs
                ));
            }
        };
    }

    private void spawnProps(Clipboard clipboard) {
        // Don't add schematicOffset here - let pasteArmorStandsOnlyFromTransformed handle the alignment
        WorldEditUtils.pasteArmorStandsOnlyFromTransformed(clipboard, location.clone().add(schematicOffset));
    }

    private PlacementOptions getEffectivePlacementOptions() {
        if (apiOptions != null) {
            return apiOptions;
        }
        return PlacementOptions.builder(BlockFace.NORTH)
                .generatorId("betterstructures")
                .build();
    }

    private String getResolvedSchematicName() {
        if (schematicContainer == null) {
            return "unknown";
        }
        String configName = schematicContainer.getConfigFilename();
        return configName.endsWith(".yml") ? configName.substring(0, configName.length() - 4) : configName;
    }

    private long getPlacementDurationMs() {
        if (!apiPlacement || apiPlacementStartTime <= 0L) {
            return 0L;
        }
        return Math.max(0L, System.currentTimeMillis() - apiPlacementStartTime);
    }

    private BlockFace getPlacementRotation() {
        return getEffectivePlacementOptions().getRotation();
    }

    private boolean isPlacementMirror() {
        return getEffectivePlacementOptions().isMirror();
    }

    private boolean shouldFillChests() {
        return getEffectivePlacementOptions().isFillChests();
    }

    private boolean shouldSpawnEntities() {
        return getEffectivePlacementOptions().isSpawnEntities();
    }

    private boolean shouldRecordInManager() {
        return getEffectivePlacementOptions().isRecordInManager();
    }

    private void completeApiResult(PlacementResult result) {
        if (!apiPlacement || apiCallback == null) {
            return;
        }
        Consumer<PlacementResult> callback = apiCallback;
        apiCallback = null;
        callback.accept(result);
    }

    private static String formatPlacementFailure(String reason) {
        if (reason == null || reason.isBlank()) {
            return "FAWE paste failed: unknown";
        }
        if (reason.startsWith("chunk_validation_failed:")) {
            return "Chunk validation failed: " + reason.substring("chunk_validation_failed:".length());
        }
        if (reason.startsWith("fawe_exception:")) {
            return "FAWE paste failed: " + reason.substring("fawe_exception:".length());
        }
        return "FAWE paste failed: " + reason;
    }

    private void assignPedestalMaterial(Location location) {
        if (this instanceof FitAirBuilding) return;
        // Handle API mode where schematicContainer may be null
        if (schematicContainer != null) {
            pedestalMaterial = schematicContainer.getSchematicConfigField().getPedestalMaterial();
        }
        Location lowestCorner = location.clone().add(schematicOffset);

        int maxSurfaceHeightScan = 20;

        //get underground pedestal blocks
        for (int x = 0; x < schematicClipboard.getDimensions().x(); x++)
            for (int z = 0; z < schematicClipboard.getDimensions().z(); z++)
                for (int y = 0; y < schematicClipboard.getDimensions().y(); y++) {
                    Block groundBlock = lowestCorner.clone().add(new Vector(x, y, z)).getBlock();
                    Block aboveBlock = groundBlock.getRelative(BlockFace.UP);

                    if (aboveBlock.getType().isSolid() && groundBlock.getType().isSolid() && !SurfaceMaterials.ignorable(groundBlock.getType()))
                        undergroundPedestalMaterials.merge(groundBlock.getType(), 1, Integer::sum);
                }

        //get above ground pedestal blocks, if any
        for (int x = 0; x < schematicClipboard.getDimensions().x(); x++)
            for (int z = 0; z < schematicClipboard.getDimensions().z(); z++) {
                boolean scanUp = lowestCorner.clone().add(new Vector(x, schematicClipboard.getDimensions().y(), z)).getBlock().getType().isSolid();
                for (int y = 0; y < maxSurfaceHeightScan; y++) {
                    Block groundBlock = lowestCorner.clone().add(new Vector(x, scanUp ? y : -y, z)).getBlock();
                    Block aboveBlock = groundBlock.getRelative(BlockFace.UP);

                    if (!aboveBlock.getType().isSolid() && groundBlock.getType().isSolid()) {
                        surfacePedestalMaterials.merge(groundBlock.getType(), 1, Integer::sum);
                        break;
                    }
                }
            }
    }

    private Material getPedestalMaterial(boolean isPedestalSurface) {
        if (isPedestalSurface) {
            if (surfacePedestalMaterials.isEmpty()) return pedestalMaterial;
            return getRandomMaterialBasedOnWeight(surfacePedestalMaterials);
        } else {
            if (undergroundPedestalMaterials.isEmpty()) return pedestalMaterial;
            return getRandomMaterialBasedOnWeight(undergroundPedestalMaterials);
        }
    }

    public Material getRandomMaterialBasedOnWeight(HashMap<Material, Integer> weightedMaterials) {
        // Calculate the total weight
        int totalWeight = weightedMaterials.values().stream().mapToInt(Integer::intValue).sum();

        // Generate a random number in the range of 0 (inclusive) to totalWeight (exclusive)
        int randomNumber = ThreadLocalRandom.current().nextInt(totalWeight);

        // Iterate through the materials and pick one based on the random number
        int cumulativeWeight = 0;
        for (Map.Entry<Material, Integer> entry : weightedMaterials.entrySet()) {
            cumulativeWeight += entry.getValue();
            if (randomNumber < cumulativeWeight) {
                return entry.getKey();
            }
        }

        // Fallback return, should not occur if the map is not empty and weights are positive
        throw new IllegalStateException("Weighted random selection failed.");
    }

    private void addPedestal(Location location) {
        if (this instanceof FitAirBuilding || this instanceof FitLiquidBuilding) return;
        Location lowestCorner = location.clone().add(schematicOffset);
        for (int x = 0; x < schematicClipboard.getDimensions().x(); x++)
            for (int z = 0; z < schematicClipboard.getDimensions().z(); z++) {
                Location blockLoc = lowestCorner.clone().add(new Vector(x, 0, z));
                // Skip if chunk not loaded to avoid sync chunk loading
                if (!blockLoc.getWorld().isChunkLoaded(blockLoc.getBlockX() >> 4, blockLoc.getBlockZ() >> 4)) {
                    continue;
                }
                //Only add pedestals for areas with a solid floor, some schematics can have rounded air edges to better fit terrain
                Block groundBlock = blockLoc.getBlock();
                if (groundBlock.getType().isAir()) continue;
                for (int y = -1; y > -11; y--) {
                    Block block = lowestCorner.clone().add(new Vector(x, y, z)).getBlock();
                    if (SurfaceMaterials.ignorable(block.getType())) {
                        // Use setBlockData with false to disable physics updates
                        Material pedestalMat = getPedestalMaterial(!block.getRelative(BlockFace.UP).getType().isSolid());
                        block.setBlockData(pedestalMat.createBlockData(), false);
                    } else {
                        //Pedestal only fills until it hits the first solid block
                        break;
                    }
                }
            }
    }

    private void clearTrees(Location location) {
        Location highestCorner = location.clone().add(schematicOffset).add(new Vector(0, schematicClipboard.getDimensions().y() + 1, 0));
        boolean detectedTreeElement = true;
        for (int x = 0; x < schematicClipboard.getDimensions().x(); x++)
            for (int z = 0; z < schematicClipboard.getDimensions().z(); z++) {
                Location blockLoc = highestCorner.clone().add(new Vector(x, 0, z));
                // Skip if chunk not loaded to avoid sync chunk loading
                if (!blockLoc.getWorld().isChunkLoaded(blockLoc.getBlockX() >> 4, blockLoc.getBlockZ() >> 4)) {
                    continue;
                }
                for (int y = 0; y < 31; y++) {
                    if (!detectedTreeElement) break;
                    detectedTreeElement = false;
                    Block block = highestCorner.clone().add(new Vector(x, y, z)).getBlock();
                    if (SurfaceMaterials.ignorable(block.getType()) && !block.getType().isAir()) {
                        detectedTreeElement = true;
                        // Use setBlockData with false to disable physics updates
                        block.setBlockData(Material.AIR.createBlockData(), false);
                    }
                }
            }
    }

    private void fillChests() {
        if (schematicContainer == null) return;

        List<Vector> chestLocations = apiPlacement ? apiChestLocations : schematicContainer.getChestLocations();
        if (schematicContainer.getGeneratorConfigFields().getChestContents() != null)
            for (Vector chestPosition : chestLocations) {
                Location chestLocation = LocationProjector.project(location, schematicOffset, chestPosition);
                if (!(chestLocation.getBlock().getState() instanceof Container container)) {
                    Logger.warn("Dự kiến " + chestLocation.getBlock().getType() + " là thùng chứa nhưng không lấy được. Bỏ qua chiến lợi phẩm này!");
                    continue;
                }

                String treasureFilename;
                if (schematicContainer.getChestContents() != null) {
                    schematicContainer.getChestContents().rollChestContents(container);
                    treasureFilename = schematicContainer.getSchematicConfigField().getTreasureFile();
                } else {
                    schematicContainer.getGeneratorConfigFields().getChestContents().rollChestContents(container);
                    treasureFilename = schematicContainer.getGeneratorConfigFields().getTreasureFilename();
                }

                ChestFillEvent chestFillEvent = new ChestFillEvent(container, treasureFilename);
                Bukkit.getServer().getPluginManager().callEvent(chestFillEvent);
                if (!chestFillEvent.isCancelled()) {
                    container.update(true);

                }
            }
    }

    private void spawnEntities() {
        if (schematicContainer == null) return;
        
        List<UUID> spawnedMobUUIDs = new ArrayList<>();
        List<MobSpawnConfig> mobSpawnConfigs = new ArrayList<>();
        Map<Vector, EntityType> vanillaSpawns = apiPlacement ? apiVanillaSpawns : schematicContainer.getVanillaSpawns();
        Map<Vector, String> eliteSpawns = apiPlacement ? apiEliteMobsSpawns : schematicContainer.getEliteMobsSpawns();
        Map<Vector, String> mythicSpawns = apiPlacement ? apiMythicMobsSpawns : schematicContainer.getMythicMobsSpawns();

        // Spawn vanilla mobs
        for (Vector entityPosition : vanillaSpawns.keySet()) {
            Location signLocation = LocationProjector.project(location, schematicOffset, entityPosition).clone();
            // Skip if chunk not loaded to avoid sync chunk loading with FAWE
            if (!signLocation.getWorld().isChunkLoaded(signLocation.getBlockX() >> 4, signLocation.getBlockZ() >> 4)) {
                continue;
            }
            // Use setBlockData with false to disable physics updates
            signLocation.getBlock().setBlockData(Material.AIR.createBlockData(), false);
            //If mobs spawn in corners they might choke on adjacent walls
            signLocation.add(new Vector(0.5, 0, 0.5));
            EntityType entityType = vanillaSpawns.get(entityPosition);

            // MythicMobs override: try to replace vanilla mob with MM equivalent
            Entity entity = null;
            MobSpawnConfig.MobType trackingType = MobSpawnConfig.MobType.VANILLA;
            String trackingIdentifier = entityType.name();

            if (MythicMobs.isOverrideActive() && DefaultConfig.isMmOverrideReplaceVanillaMobs()
                    && ThreadLocalRandom.current().nextDouble(100) < DefaultConfig.getVanillaReplaceChance()) {
                String mmMobId = MythicMobs.getRandomMobByType(entityType);
                if (mmMobId != null) {
                    entity = MythicMobs.spawnAndReturn(signLocation, mmMobId + ":1");
                    if (entity != null) {
                        trackingType = MobSpawnConfig.MobType.VANILLA_MM_OVERRIDE;
                        // Store original EntityType name as identifier for re-selection on respawn
                        trackingIdentifier = entityType.name();
                    }
                }
            }

            // Fallback to vanilla spawning if MM override didn't work
            if (entity == null) {
                entity = signLocation.getWorld().spawnEntity(signLocation, entityType);
                entity.setPersistent(true);
                if (entity instanceof LivingEntity) {
                    ((LivingEntity) entity).setRemoveWhenFarAway(false);
                }
                trackingType = MobSpawnConfig.MobType.VANILLA;
                trackingIdentifier = entityType.name();
            }

            if (!VersionChecker.serverVersionOlderThan(21, 0) &&
                    entity.getType().equals(EntityType.END_CRYSTAL)) {
                EnderCrystal enderCrystal = (EnderCrystal) entity;
                enderCrystal.setShowingBottom(false);
            }

            // Track mob for respawning (only LivingEntities)
            if (entity instanceof LivingEntity && DefaultConfig.isMobTrackingEnabled()) {
                Vector actualOffset = schematicOffset.clone().add(entityPosition);
                spawnedMobUUIDs.add(entity.getUniqueId());
                mobSpawnConfigs.add(new MobSpawnConfig(
                        trackingType,
                        trackingIdentifier,
                        actualOffset.getX(),
                        actualOffset.getY(),
                        actualOffset.getZ()
                ));
            }
        }

        // Spawn EliteMobs bosses
        for (Vector elitePosition : eliteSpawns.keySet()) {
            Location eliteLocation = LocationProjector.project(location, schematicOffset, elitePosition).clone();
            // Skip if chunk not loaded to avoid sync chunk loading with FAWE
            if (!eliteLocation.getWorld().isChunkLoaded(eliteLocation.getBlockX() >> 4, eliteLocation.getBlockZ() >> 4)) {
                continue;
            }
            eliteLocation.getBlock().setBlockData(Material.AIR.createBlockData(), false);
            eliteLocation.add(new Vector(0.5, 0, 0.5));
            String bossFilename = eliteSpawns.get(elitePosition);

            Entity eliteMob = null;
            MobSpawnConfig.MobType trackingType = MobSpawnConfig.MobType.ELITEMOBS;
            String trackingIdentifier = bossFilename;

            // MythicMobs override: try to replace EM boss with MM boss
            if (MythicMobs.isOverrideActive() && DefaultConfig.isMmOverrideReplaceEliteMobsBosses()) {
                String mmBossId = MythicMobs.getRandomBoss();
                if (mmBossId != null) {
                    eliteMob = MythicMobs.spawnAndReturn(eliteLocation, mmBossId + ":1");
                    if (eliteMob != null) {
                        trackingType = MobSpawnConfig.MobType.ELITEMOBS_MM_OVERRIDE;
                        trackingIdentifier = mmBossId;
                    }
                } else {
                    Logger.warn("Danh sách MythicMobs Boss trống! Không thể thay thế EliteMobs Boss: " + bossFilename);
                }
            }

            // Fallback to original EliteMobs spawning
            if (eliteMob == null) {
                eliteMob = EliteMobs.spawnAndReturn(eliteLocation, bossFilename);
                if (eliteMob == null) return;
                trackingType = MobSpawnConfig.MobType.ELITEMOBS;
                trackingIdentifier = bossFilename;
            }

            // Track mob
            if (DefaultConfig.isMobTrackingEnabled()) {
                Vector actualOffset = schematicOffset.clone().add(elitePosition);
                spawnedMobUUIDs.add(eliteMob.getUniqueId());
                mobSpawnConfigs.add(new MobSpawnConfig(
                        trackingType,
                        trackingIdentifier,
                        actualOffset.getX(),
                        actualOffset.getY(),
                        actualOffset.getZ()
                ));
            }

            // Only set up WorldGuard protection for original EliteMobs bosses (not MM overrides)
            if (trackingType == MobSpawnConfig.MobType.ELITEMOBS) {
                Location lowestCorner = location.clone().add(schematicOffset);
                Location highestCorner = lowestCorner.clone().add(new Vector(schematicClipboard.getRegion().getWidth() - 1, schematicClipboard.getRegion().getHeight(), schematicClipboard.getRegion().getLength() - 1));
                if (DefaultConfig.isProtectEliteMobsRegions() &&
                        Bukkit.getPluginManager().getPlugin("WorldGuard") != null &&
                        Bukkit.getPluginManager().getPlugin("EliteMobs") != null) {
                    WorldGuard.Protect(lowestCorner, highestCorner, bossFilename, eliteLocation);
                } else {
                    if (!worldGuardWarn) {
                        worldGuardWarn = true;
                        Logger.warn("Bạn không sử dụng WorldGuard, do đó BetterStructures không thể bảo vệ đấu trường Boss! Khuyên dùng WorldGuard để đảm bảo trải nghiệm chiến đấu công bằng.");
                    }
                }
            }
        }

        // Spawn MythicMobs
        for (Map.Entry<Vector, String> entry : mythicSpawns.entrySet()) {
            Location mobLocation = LocationProjector.project(location, schematicOffset, entry.getKey()).clone();
            // Skip if chunk not loaded to avoid sync chunk loading with FAWE
            if (!mobLocation.getWorld().isChunkLoaded(mobLocation.getBlockX() >> 4, mobLocation.getBlockZ() >> 4)) {
                continue;
            }
            mobLocation.getBlock().setBlockData(Material.AIR.createBlockData(), false);

            // Use spawnAndReturn to get the entity
            Entity mythicMob = MythicMobs.spawnAndReturn(mobLocation, entry.getValue());
            if (mythicMob == null) return;

            // Track mob for respawning
            // Store actual offset including schematicOffset
            if (DefaultConfig.isMobTrackingEnabled()) {
                Vector actualOffset = schematicOffset.clone().add(entry.getKey());
                spawnedMobUUIDs.add(mythicMob.getUniqueId());
                mobSpawnConfigs.add(new MobSpawnConfig(
                        MobSpawnConfig.MobType.MYTHICMOBS,
                        entry.getValue(),
                        actualOffset.getX(),
                        actualOffset.getY(),
                        actualOffset.getZ()
                ));
            }
        }

        // Register mobs with tracking manager
        if (DefaultConfig.isMobTrackingEnabled() && !spawnedMobUUIDs.isEmpty()) {
            // Get or create structure location data
            StructureLocationData structureData = StructureLocationManager.getInstance()
                    .getStructureAt(location.getWorld().getName(), location.getBlockX(), location.getBlockY(), location.getBlockZ());

            if (structureData != null) {
                MobTrackingManager.getInstance().registerStructureMobs(structureData, spawnedMobUUIDs, mobSpawnConfigs);
                // Set boss structure flag
                structureData.setBossStructure(schematicContainer.isBossStructure());
                StructureLocationManager.getInstance().markDirty(location.getWorld().getName());
            }
        }
    }
}

package com.magmaguy.betterstructures.api;

import com.magmaguy.betterstructures.MetadataHandler;
import com.magmaguy.betterstructures.buildingfitter.FitAnything;
import com.magmaguy.betterstructures.schematics.SchematicContainer;
import com.magmaguy.betterstructures.structurelocation.StructureLocationData;
import com.magmaguy.betterstructures.structurelocation.StructureLocationManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Public BetterStructures API for programmatic placements and queries.
 */
public final class BetterStructuresAPI {
    private BetterStructuresAPI() {
    }

    public static CompletableFuture<PlacementResult> placeStructure(
            String schematicName,
            Location location,
            PlacementOptions options
    ) {
        if (schematicName == null || schematicName.isBlank()) {
            return CompletableFuture.completedFuture(failureResult(
                    schematicName,
                    location,
                    options,
                    "Schematic name cannot be null or empty",
                    0L
            ));
        }

        if (location == null || location.getWorld() == null) {
            return CompletableFuture.completedFuture(failureResult(
                    schematicName,
                    location,
                    options,
                    "World not loaded: null",
                    0L
            ));
        }

        if (options == null) {
            return CompletableFuture.completedFuture(failureResult(
                    schematicName,
                    location,
                    PlacementOptions.builder(BlockFace.NORTH).build(),
                    "PlacementOptions cannot be null",
                    0L
            ));
        }

        if (MetadataHandler.PLUGIN == null || !MetadataHandler.PLUGIN.isEnabled()) {
            return CompletableFuture.completedFuture(failureResult(
                    schematicName,
                    location,
                    options,
                    "BetterStructures plugin is not enabled",
                    0L
            ));
        }

        Optional<SchematicContainer> schematicContainer = resolveSchematicContainer(schematicName);
        if (schematicContainer.isEmpty()) {
            return CompletableFuture.completedFuture(failureResult(
                    schematicName,
                    location,
                    options,
                    "Schematic not found: " + schematicName,
                    0L
            ));
        }

        CompletableFuture<PlacementResult> future = new CompletableFuture<>();
        Runnable placementTask = () -> {
            try {
                FitAnything fitAnything = new FitAnything(
                        schematicContainer.get(),
                        location.clone(),
                        options,
                        future::complete
                );
                fitAnything.pasteDirect(location.clone());
            } catch (Exception exception) {
                future.complete(failureResult(
                        schematicName,
                        location,
                        options,
                        "Placement setup failed: " + exception.getMessage(),
                        0L
                ));
            }
        };

        if (Bukkit.isPrimaryThread()) {
            placementTask.run();
        } else {
            Bukkit.getScheduler().runTask(MetadataHandler.PLUGIN, placementTask);
        }

        return future;
    }

    public static CompletableFuture<List<PlacementResult>> placeStructures(
            Map<String, Location> placements,
            PlacementOptions defaultOptions,
            boolean throttled,
            int tickDelay
    ) {
        if (placements == null || placements.isEmpty()) {
            return CompletableFuture.completedFuture(List.of());
        }

        PlacementOptions safeOptions = defaultOptions == null
                ? PlacementOptions.builder(BlockFace.NORTH).build()
                : defaultOptions;

        List<Map.Entry<String, Location>> entries = new ArrayList<>(placements.entrySet());
        List<CompletableFuture<PlacementResult>> placementFutures = new ArrayList<>();

        if (!throttled || MetadataHandler.PLUGIN == null || !MetadataHandler.PLUGIN.isEnabled()) {
            for (Map.Entry<String, Location> entry : entries) {
                placementFutures.add(placeStructure(entry.getKey(), entry.getValue(), safeOptions));
            }
            return combinePlacementFutures(placementFutures);
        }

        CompletableFuture<List<PlacementResult>> aggregate = new CompletableFuture<>();
        scheduleThrottledPlacements(entries, safeOptions, Math.max(1, tickDelay), 0, placementFutures, aggregate);
        return aggregate;
    }

    public static List<StructureLocationData> getNearbyStructures(Location center, double radius) {
        if (center == null || center.getWorld() == null || radius < 0) {
            return List.of();
        }

        return StructureLocationManager.getInstance().getNearbyStructures(center, radius);
    }

    public static List<StructureLocationData> getStructuresByName(String worldName, String schematicPattern) {
        if (worldName == null || worldName.isBlank() || schematicPattern == null || schematicPattern.isBlank()) {
            return List.of();
        }

        return StructureLocationManager.getInstance().getStructuresByName(worldName, schematicPattern);
    }

    public static Optional<SchematicInfo> getSchematicInfo(String schematicName) {
        return resolveSchematicContainer(schematicName).map(SchematicInfo::fromSchematic);
    }

    public static boolean isSchematicAvailable(String schematicName) {
        return resolveSchematicContainer(schematicName).isPresent();
    }

    private static CompletableFuture<List<PlacementResult>> combinePlacementFutures(
            List<CompletableFuture<PlacementResult>> placementFutures
    ) {
        CompletableFuture<?>[] futures = placementFutures.toArray(new CompletableFuture[0]);
        return CompletableFuture.allOf(futures).thenApply(ignored ->
                placementFutures.stream()
                        .map(CompletableFuture::join)
                        .collect(Collectors.toList()));
    }

    private static void scheduleThrottledPlacements(
            List<Map.Entry<String, Location>> entries,
            PlacementOptions options,
            int tickDelay,
            int index,
            List<CompletableFuture<PlacementResult>> placementFutures,
            CompletableFuture<List<PlacementResult>> aggregate
    ) {
        if (index >= entries.size()) {
            combinePlacementFutures(placementFutures)
                    .whenComplete((results, throwable) -> {
                        if (throwable != null) {
                            aggregate.completeExceptionally(throwable);
                            return;
                        }
                        aggregate.complete(results);
                    });
            return;
        }

        Map.Entry<String, Location> entry = entries.get(index);
        placementFutures.add(placeStructure(entry.getKey(), entry.getValue(), options));

        if (index == entries.size() - 1) {
            scheduleThrottledPlacements(entries, options, tickDelay, index + 1, placementFutures, aggregate);
            return;
        }

        Bukkit.getScheduler().runTaskLater(
                MetadataHandler.PLUGIN,
                () -> scheduleThrottledPlacements(entries, options, tickDelay, index + 1, placementFutures, aggregate),
                tickDelay
        );
    }

    private static Optional<SchematicContainer> resolveSchematicContainer(String schematicName) {
        if (schematicName == null || schematicName.isBlank()) {
            return Optional.empty();
        }

        String normalized = normalizeSchematicToken(schematicName);
        Collection<SchematicContainer> uniqueContainers = new LinkedHashSet<>(SchematicContainer.getSchematics().values());
        for (SchematicContainer container : uniqueContainers) {
            if (matchesSchematicName(container, normalized)) {
                return Optional.of(container);
            }
        }
        return Optional.empty();
    }

    private static boolean matchesSchematicName(SchematicContainer container, String normalized) {
        return normalizeSchematicToken(container.getClipboardFilename()).equals(normalized)
                || normalizeSchematicToken(container.getConfigFilename()).equals(normalized);
    }

    private static String normalizeSchematicToken(String schematicName) {
        String normalized = schematicName.toLowerCase(Locale.ROOT).trim();
        if (normalized.endsWith(".schem")) {
            normalized = normalized.substring(0, normalized.length() - 6);
        }
        if (normalized.endsWith(".yml")) {
            normalized = normalized.substring(0, normalized.length() - 4);
        }
        return normalized;
    }

    public static boolean matchesPattern(String schematicName, String pattern) {
        String normalizedName = normalizeSchematicToken(schematicName);
        String normalizedPattern = normalizeSchematicToken(pattern);
        if (!normalizedPattern.contains("*")) {
            return normalizedName.contains(normalizedPattern);
        }

        String regex = normalizedPattern.replace(".", "\\.").replace("*", ".*");
        return Pattern.compile(regex).matcher(normalizedName).matches();
    }

    private static PlacementResult failureResult(
            String schematicName,
            Location location,
            PlacementOptions options,
            String errorMessage,
            long placementTimeMs
    ) {
        BlockFace rotation = options == null ? BlockFace.NORTH : options.getRotation();
        boolean mirror = options != null && options.isMirror();
        return PlacementResult.failure(
                normalizeFailureSchematicName(schematicName),
                location,
                rotation,
                mirror,
                errorMessage,
                placementTimeMs
        );
    }

    private static String normalizeFailureSchematicName(String schematicName) {
        if (schematicName == null || schematicName.isBlank()) {
            return "unknown";
        }
        return normalizeSchematicToken(schematicName);
    }
}

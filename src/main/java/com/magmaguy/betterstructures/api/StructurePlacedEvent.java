package com.magmaguy.betterstructures.api;

import com.magmaguy.betterstructures.structurelocation.StructureLocationData;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Fired after a structure paste fully succeeds and is optionally recorded.
 */
public class StructurePlacedEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();

    private final StructureLocationData structureData;
    private final PlacementOptions options;
    private final long placementTimeMs;

    public StructurePlacedEvent(
            StructureLocationData structureData,
            PlacementOptions options,
            long placementTimeMs
    ) {
        this.structureData = structureData;
        this.options = options;
        this.placementTimeMs = placementTimeMs;
    }

    public StructureLocationData getStructureData() {
        return structureData;
    }

    public PlacementOptions getOptions() {
        return options;
    }

    public long getPlacementTimeMs() {
        return placementTimeMs;
    }

    @NotNull
    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}

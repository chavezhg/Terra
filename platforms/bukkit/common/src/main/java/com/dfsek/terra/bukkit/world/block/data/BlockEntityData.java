/*
 * This file is part of Terra.
 *
 * Terra is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Terra is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Terra.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.dfsek.terra.bukkit.world.block.data;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.loot.LootTable;
import org.bukkit.loot.Lootable;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Block entity data attached to a block in a config pack, e.g. {@code minecraft:chest{LootTable:'chests/simple_dungeon'}}.
 * <p>
 * Bukkit's {@code createBlockData} only understands block state properties, and rejects any trailing NBT outright, so the
 * NBT is split off up front, represented here, and applied through the Bukkit API once the block has been placed.
 */
public class BlockEntityData {
    private static final Logger logger = LoggerFactory.getLogger(BlockEntityData.class);

    /**
     * Matches the LootTable entry of an SNBT compound, quoted with either ' or ".
     */
    private static final Pattern LOOT_TABLE = Pattern.compile("LootTable\\s*:\\s*['\"]([^'\"]+)['\"]");

    private final String snbt;
    private final LootTable lootTable;

    private BlockEntityData(String snbt, LootTable lootTable) {
        this.snbt = snbt;
        this.lootTable = lootTable;
    }

    /**
     * Parses the SNBT attached to a block, returning null if it holds nothing Terra knows how to apply.
     */
    public static @Nullable BlockEntityData parse(String snbt) {
        Matcher matcher = LOOT_TABLE.matcher(snbt);
        if(!matcher.find()) {
            logger.debug("Ignoring unsupported block entity data: {}", snbt);
            return null;
        }

        NamespacedKey key = NamespacedKey.fromString(matcher.group(1));
        if(key == null) {
            logger.warn("Invalid loot table key in block entity data: {}", snbt);
            return null;
        }

        LootTable lootTable = Bukkit.getLootTable(key);
        if(lootTable == null) {
            logger.warn("Unknown loot table \"{}\" in block entity data: {}", key, snbt);
            return null;
        }

        return new BlockEntityData(snbt, lootTable);
    }

    /**
     * Applies this data to an already-placed block. The state must be fetched from the world/region the block was placed
     * in, so that {@link org.bukkit.block.BlockState#update} writes back to the right place.
     */
    public void applyTo(org.bukkit.block.BlockState state) {
        if(state instanceof Lootable lootable) {
            lootable.setLootTable(lootTable);
            state.update(true, false);
        } else {
            logger.warn("Block {} cannot hold a loot table, ignoring: {}", state.getType(), snbt);
        }
    }

    @Override
    public String toString() {
        return snbt;
    }
}

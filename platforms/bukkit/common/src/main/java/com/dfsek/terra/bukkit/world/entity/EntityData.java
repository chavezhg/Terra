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

package com.dfsek.terra.bukkit.world.entity;

import org.bukkit.entity.EnderCrystal;
import org.bukkit.entity.Entity;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * NBT attached to an entity in a config pack, e.g. {@code minecraft:end_crystal{ShowBottom:0}}.
 * <p>
 * Bukkit spawns entities from an {@link org.bukkit.entity.EntityType} enum constant alone, and its lookup rejects any
 * trailing NBT, so the NBT is split off up front, represented here, and applied through the Bukkit API once the entity
 * has been spawned.
 */
public class EntityData {
    private static final Logger logger = LoggerFactory.getLogger(EntityData.class);

    /**
     * Matches the ShowBottom entry of an SNBT compound, accepting both the byte form ({@code 0b}) and boolean form.
     */
    private static final Pattern SHOW_BOTTOM = Pattern.compile("ShowBottom\\s*:\\s*(1|0|true|false)b?",
        Pattern.CASE_INSENSITIVE);

    private final String snbt;
    private final boolean showBottom;

    private EntityData(String snbt, boolean showBottom) {
        this.snbt = snbt;
        this.showBottom = showBottom;
    }

    /**
     * Parses the SNBT attached to an entity, returning null if it holds nothing Terra knows how to apply.
     */
    public static @Nullable EntityData parse(String snbt) {
        Matcher matcher = SHOW_BOTTOM.matcher(snbt);
        if(!matcher.find()) {
            logger.debug("Ignoring unsupported entity data: {}", snbt);
            return null;
        }

        String value = matcher.group(1);
        return new EntityData(snbt, value.equals("1") || value.equalsIgnoreCase("true"));
    }

    /**
     * Applies this data to an already-spawned entity.
     */
    public void applyTo(Entity entity) {
        if(entity instanceof EnderCrystal crystal) {
            crystal.setShowingBottom(showBottom);
        } else {
            logger.warn("Entity {} cannot show a bottom, ignoring: {}", entity.getType(), snbt);
        }
    }

    @Override
    public String toString() {
        return snbt;
    }
}

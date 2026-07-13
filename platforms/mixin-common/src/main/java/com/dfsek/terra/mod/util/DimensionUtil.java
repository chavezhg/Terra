package com.dfsek.terra.mod.util;

import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;
import net.minecraft.world.attribute.BedRule;
import net.minecraft.world.attribute.EnvironmentAttributeMap;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.dimension.DimensionType.MonsterSettings;
import net.minecraft.world.dimension.DimensionType.Skybox;
import org.jetbrains.annotations.NotNull;

import com.dfsek.terra.mod.ModPlatform;
import com.dfsek.terra.mod.config.MonsterSettingsConfig;
import com.dfsek.terra.mod.config.VanillaWorldProperties;
import com.dfsek.terra.mod.implmentation.TerraIntProvider;


public class DimensionUtil {
    public static DimensionType createDimension(VanillaWorldProperties vanillaWorldProperties, DimensionType defaultDimension,
                                                ModPlatform platform) {

        MonsterSettingsConfig monsterSettingsConfig;
        if(vanillaWorldProperties.getMonsterSettings() != null) {
            monsterSettingsConfig = vanillaWorldProperties.getMonsterSettings();
        } else {
            monsterSettingsConfig = new MonsterSettingsConfig();
        }

        MonsterSettings monsterSettings = getMonsterSettings(defaultDimension, monsterSettingsConfig);

        return new DimensionType(
            // 1.21.11 no longer stores the fixed time as a tick value; the time of day is driven by the dimension's
            // Timelines, and DimensionType only records *whether* time is fixed.
            // TODO: honouring an arbitrary `minecraft.fixed-time` tick now requires registering a custom Timeline.
            vanillaWorldProperties.getFixedTime() == null ? defaultDimension.hasFixedTime() : true,
            vanillaWorldProperties.getHasSkyLight() == null ? defaultDimension.hasSkyLight() : vanillaWorldProperties.getHasSkyLight(),
            vanillaWorldProperties.getHasCeiling() == null ? defaultDimension.hasCeiling() : vanillaWorldProperties.getHasCeiling(),
            vanillaWorldProperties.getCoordinateScale() == null
            ? defaultDimension.coordinateScale()
            : vanillaWorldProperties.getCoordinateScale(),
            vanillaWorldProperties.getHeight() == null ? defaultDimension.minY() : vanillaWorldProperties.getHeight().getMin(),
            vanillaWorldProperties.getHeight() == null ? defaultDimension.height() : vanillaWorldProperties.getHeight().getRange(),
            vanillaWorldProperties.getLogicalHeight() == null
            ? defaultDimension.logicalHeight()
            : vanillaWorldProperties.getLogicalHeight(),
            vanillaWorldProperties.getInfiniburn() == null
            ? defaultDimension.infiniburn()
            : TagKey.of(RegistryKeys.BLOCK, vanillaWorldProperties.getInfiniburn()),
            vanillaWorldProperties.getAmbientLight() == null ? defaultDimension.ambientLight() : vanillaWorldProperties.getAmbientLight(),
            monsterSettings,
            getSkybox(defaultDimension, vanillaWorldProperties.getEffects()),
            defaultDimension.cardinalLightType(),
            getAttributes(defaultDimension, vanillaWorldProperties, monsterSettingsConfig),
            defaultDimension.timelines()
        );
    }

    /**
     * The dimension's visual effects are no longer selected by an identifier; 1.21.11 replaced them with the Skybox
     * enum, and everything else the old effects controlled moved into the environment attributes.
     */
    private static Skybox getSkybox(DimensionType defaultDimension, Identifier effects) {
        if(effects == null) return defaultDimension.skybox();

        return switch(effects.toString()) {
            case "minecraft:overworld" -> Skybox.OVERWORLD;
            case "minecraft:the_end" -> Skybox.END;
            case "minecraft:the_nether" -> Skybox.NONE;
            default -> defaultDimension.skybox();
        };
    }

    /**
     * A number of properties that used to live directly on DimensionType / MonsterSettings became environment
     * attributes in 1.21.11. Inherit the parent dimension's attribute map, then override the ones the config sets.
     */
    private static EnvironmentAttributeMap getAttributes(DimensionType defaultDimension, VanillaWorldProperties properties,
                                                         MonsterSettingsConfig monsterSettings) {
        EnvironmentAttributeMap.Builder attributes = EnvironmentAttributeMap.builder().addAll(defaultDimension.attributes());

        if(properties.getBedWorks() != null) {
            attributes.with(EnvironmentAttributes.BED_RULE_GAMEPLAY,
                properties.getBedWorks() ? BedRule.OVERWORLD : BedRule.OTHER_DIMENSION);
        }

        if(properties.getRespawnAnchorWorks() != null) {
            attributes.with(EnvironmentAttributes.RESPAWN_ANCHOR_WORKS_GAMEPLAY, properties.getRespawnAnchorWorks());
        }

        if(properties.getCloudHeight() != null) {
            attributes.with(EnvironmentAttributes.CLOUD_HEIGHT_VISUAL, properties.getCloudHeight().floatValue());
        }

        // `ultrawarm` was decomposed into the individual behaviours it used to imply.
        if(properties.getUltraWarm() != null) {
            attributes.with(EnvironmentAttributes.WATER_EVAPORATES_GAMEPLAY, properties.getUltraWarm())
                      .with(EnvironmentAttributes.FAST_LAVA_GAMEPLAY, properties.getUltraWarm())
                      .with(EnvironmentAttributes.INCREASED_FIRE_BURNOUT_GAMEPLAY, properties.getUltraWarm());
        }

        // A non-natural dimension is the one where nether portals spawn zombified piglins.
        if(properties.getNatural() != null) {
            attributes.with(EnvironmentAttributes.NETHER_PORTAL_SPAWNS_PIGLIN_GAMEPLAY, !properties.getNatural());
        }

        if(monsterSettings.getPiglinSafe() != null) {
            attributes.with(EnvironmentAttributes.PIGLINS_ZOMBIFY_GAMEPLAY, !monsterSettings.getPiglinSafe());
        }

        if(monsterSettings.getHasRaids() != null) {
            attributes.with(EnvironmentAttributes.CAN_START_RAID_GAMEPLAY, monsterSettings.getHasRaids());
        }

        return attributes.build();
    }

    @NotNull
    private static MonsterSettings getMonsterSettings(DimensionType defaultDimension, MonsterSettingsConfig monsterSettingsConfig) {
        MonsterSettings defaultMonsterSettings = defaultDimension.monsterSettings();

        return new MonsterSettings(
            monsterSettingsConfig.getMonsterSpawnLight() == null ? defaultMonsterSettings.monsterSpawnLightTest() : new TerraIntProvider(
                monsterSettingsConfig.getMonsterSpawnLight()),
            monsterSettingsConfig.getMonsterSpawnBlockLightLimit() == null
            ? defaultMonsterSettings.monsterSpawnBlockLightLimit()
            : monsterSettingsConfig.getMonsterSpawnBlockLightLimit()
        );
    }
}

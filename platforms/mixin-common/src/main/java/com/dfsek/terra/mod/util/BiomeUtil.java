package com.dfsek.terra.mod.util;

import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.world.attribute.AmbientSounds;
import net.minecraft.world.attribute.BackgroundMusic;
import net.minecraft.world.attribute.EnvironmentAttribute;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.Biome.Builder;
import net.minecraft.world.biome.BiomeEffects;
import net.minecraft.world.biome.GenerationSettings;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import com.dfsek.terra.api.config.ConfigPack;
import com.dfsek.terra.mod.config.VanillaBiomeProperties;
import com.dfsek.terra.mod.mixin.access.BiomeAccessor;


public class BiomeUtil {
    public static final Map<Identifier, List<Identifier>>
        TERRA_BIOME_MAP = new HashMap<>();

    private static <Value> void setAttributeIfNotNull(Biome.Builder builder, EnvironmentAttribute<Value> attribute, Value value) {
        if(value == null) return;
        builder.setEnvironmentAttribute(attribute, value);
    }

    public static Biome createBiome(Biome vanilla, VanillaBiomeProperties vanillaBiomeProperties) {
        BiomeEffects.Builder effects = new BiomeEffects.Builder();

        net.minecraft.world.biome.Biome.Builder builder = new Builder();

        // As of 1.21.11 fog/sky/water-fog colors, particles, ambient sounds and music are no longer part of
        // BiomeEffects; they live in the biome's EnvironmentAttributeMap. Inherit the parent biome's attributes,
        // then override only the ones the config actually specifies.
        builder.addEnvironmentAttributes(vanilla.getEnvironmentAttributes());

        effects.waterColor(Objects.requireNonNullElse(vanillaBiomeProperties.getWaterColor(), vanilla.getWaterColor()))
            .grassColorModifier(
                Objects.requireNonNullElse(vanillaBiomeProperties.getGrassColorModifier(), vanilla.getEffects().grassColorModifier()));

        if(vanillaBiomeProperties.getGrassColor() == null) {
            vanilla.getEffects().grassColor().ifPresent(effects::grassColor);
        } else {
            effects.grassColor(vanillaBiomeProperties.getGrassColor());
        }

        if(vanillaBiomeProperties.getFoliageColor() == null) {
            vanilla.getEffects().foliageColor().ifPresent(effects::foliageColor);
        } else {
            effects.foliageColor(vanillaBiomeProperties.getFoliageColor());
        }

        if(vanillaBiomeProperties.getDryFoliageColor() == null) {
            vanilla.getEffects().dryFoliageColor().ifPresent(effects::dryFoliageColor);
        } else {
            effects.dryFoliageColor(vanillaBiomeProperties.getDryFoliageColor());
        }

        setAttributeIfNotNull(builder, EnvironmentAttributes.FOG_COLOR_VISUAL, vanillaBiomeProperties.getFogColor());
        setAttributeIfNotNull(builder, EnvironmentAttributes.WATER_FOG_COLOR_VISUAL, vanillaBiomeProperties.getWaterFogColor());
        setAttributeIfNotNull(builder, EnvironmentAttributes.SKY_COLOR_VISUAL, vanillaBiomeProperties.getSkyColor());
        setAttributeIfNotNull(builder, EnvironmentAttributes.MUSIC_VOLUME_AUDIO, vanillaBiomeProperties.getMusicVolume());

        if(vanillaBiomeProperties.getParticleConfig() != null) {
            builder.setEnvironmentAttribute(EnvironmentAttributes.AMBIENT_PARTICLES_VISUAL,
                List.of(vanillaBiomeProperties.getParticleConfig()));
        }

        if(vanillaBiomeProperties.getMusic() != null) {
            builder.setEnvironmentAttribute(EnvironmentAttributes.BACKGROUND_MUSIC_AUDIO,
                new BackgroundMusic(vanillaBiomeProperties.getMusic()));
        }

        // The three ambient sound slots were merged into a single AmbientSounds attribute, so an override of any one of
        // them has to be merged with whatever the parent biome already had.
        if(vanillaBiomeProperties.getLoopSound() != null
           || vanillaBiomeProperties.getMoodSound() != null
           || vanillaBiomeProperties.getAdditionsSound() != null) {
            AmbientSounds inherited = vanilla.getEnvironmentAttributes()
                                             .apply(EnvironmentAttributes.AMBIENT_SOUNDS_AUDIO, AmbientSounds.DEFAULT);

            builder.setEnvironmentAttribute(EnvironmentAttributes.AMBIENT_SOUNDS_AUDIO, new AmbientSounds(
                vanillaBiomeProperties.getLoopSound() == null
                ? inherited.loop()
                : Optional.of(Registries.SOUND_EVENT.getEntry(vanillaBiomeProperties.getLoopSound())),
                vanillaBiomeProperties.getMoodSound() == null
                ? inherited.mood()
                : Optional.of(vanillaBiomeProperties.getMoodSound()),
                vanillaBiomeProperties.getAdditionsSound() == null
                ? inherited.additions()
                : List.of(vanillaBiomeProperties.getAdditionsSound())));
        }

        builder.precipitation(Objects.requireNonNullElse(vanillaBiomeProperties.getPrecipitation(), vanilla.hasPrecipitation()));

        builder.temperature(Objects.requireNonNullElse(vanillaBiomeProperties.getTemperature(), vanilla.getTemperature()));

        builder.downfall(Objects.requireNonNullElse(vanillaBiomeProperties.getDownfall(),
            ((BiomeAccessor) ((Object) vanilla)).getWeather().downfall()));

        builder.temperatureModifier(Objects.requireNonNullElse(vanillaBiomeProperties.getTemperatureModifier(),
            ((BiomeAccessor) ((Object) vanilla)).getWeather().temperatureModifier()));

        builder.spawnSettings(Objects.requireNonNullElse(vanillaBiomeProperties.getSpawnSettings(), vanilla.getSpawnSettings()));

        return builder
            .effects(effects.build())
            .generationSettings(new GenerationSettings.Builder().build())
            .build();
    }

    public static String createBiomeID(ConfigPack pack, com.dfsek.terra.api.registry.key.RegistryKey biomeID) {
        return pack.getID()
                   .toLowerCase() + "/" + biomeID.getNamespace().toLowerCase(Locale.ROOT) + "/" + biomeID.getID().toLowerCase(Locale.ROOT);
    }

    public static Map<Identifier, List<Identifier>> getTerraBiomeMap() {
        return Map.copyOf(TERRA_BIOME_MAP);
    }
}

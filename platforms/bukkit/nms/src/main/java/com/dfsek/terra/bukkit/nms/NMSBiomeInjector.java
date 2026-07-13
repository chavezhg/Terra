package com.dfsek.terra.bukkit.nms;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.attribute.AmbientSounds;
import net.minecraft.world.attribute.BackgroundMusic;
import net.minecraft.world.attribute.EnvironmentAttribute;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.biome.BiomeSpecialEffects;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

import com.dfsek.terra.api.config.ConfigPack;
import com.dfsek.terra.bukkit.nms.config.VanillaBiomeProperties;


public class NMSBiomeInjector {

    public static <T> Optional<Holder<T>> getEntry(Registry<T> registry, Identifier identifier) {
        return registry.getOptional(identifier)
            .flatMap(registry::getResourceKey)
            .flatMap(registry::get);
    }

    private static <Value> void setAttributeIfNotNull(Biome.BiomeBuilder builder, EnvironmentAttribute<Value> attribute, Value value) {
        if(value == null) return;
        builder.setAttribute(attribute, value);
    }

    public static Biome createBiome(Biome vanilla, VanillaBiomeProperties vanillaBiomeProperties)
    throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
        Biome.BiomeBuilder builder = new Biome.BiomeBuilder();

        BiomeSpecialEffects.Builder effects = new BiomeSpecialEffects.Builder();

        // As of 1.21.11 fog/sky/water-fog colors, particles, ambient sounds and music are no longer part of
        // BiomeSpecialEffects; they live in the biome's EnvironmentAttributeMap. Inherit the vanilla biome's
        // attributes, then override only the ones the config actually specifies.
        builder.putAttributes(vanilla.getAttributes());

        effects.waterColor(Objects.requireNonNullElse(vanillaBiomeProperties.getWaterColor(), vanilla.getWaterColor()))
            .grassColorModifier(Objects.requireNonNullElse(vanillaBiomeProperties.getGrassColorModifier(),
                vanilla.getSpecialEffects().grassColorModifier()));

        if(vanillaBiomeProperties.getGrassColor() == null) {
            vanilla.getSpecialEffects().grassColorOverride().ifPresent(effects::grassColorOverride);
        } else {
            effects.grassColorOverride(vanillaBiomeProperties.getGrassColor());
        }

        if(vanillaBiomeProperties.getFoliageColor() == null) {
            vanilla.getSpecialEffects().foliageColorOverride().ifPresent(effects::foliageColorOverride);
        } else {
            effects.foliageColorOverride(vanillaBiomeProperties.getFoliageColor());
        }

        setAttributeIfNotNull(builder, EnvironmentAttributes.FOG_COLOR, vanillaBiomeProperties.getFogColor());
        setAttributeIfNotNull(builder, EnvironmentAttributes.WATER_FOG_COLOR, vanillaBiomeProperties.getWaterFogColor());
        setAttributeIfNotNull(builder, EnvironmentAttributes.SKY_COLOR, vanillaBiomeProperties.getSkyColor());
        setAttributeIfNotNull(builder, EnvironmentAttributes.MUSIC_VOLUME, vanillaBiomeProperties.getMusicVolume());

        if(vanillaBiomeProperties.getParticleConfig() != null) {
            builder.setAttribute(EnvironmentAttributes.AMBIENT_PARTICLES, List.of(vanillaBiomeProperties.getParticleConfig()));
        }

        if(vanillaBiomeProperties.getMusic() != null) {
            builder.setAttribute(EnvironmentAttributes.BACKGROUND_MUSIC, new BackgroundMusic(vanillaBiomeProperties.getMusic()));
        }

        // The three ambient sound slots were merged into a single AmbientSounds attribute, so an override of any one of
        // them has to be merged with whatever the vanilla biome already had.
        if(vanillaBiomeProperties.getLoopSound() != null
           || vanillaBiomeProperties.getMoodSound() != null
           || vanillaBiomeProperties.getAdditionsSound() != null) {
            AmbientSounds inherited = vanilla.getAttributes().applyModifier(EnvironmentAttributes.AMBIENT_SOUNDS, AmbientSounds.EMPTY);

            Optional<Holder<SoundEvent>> loop =
                vanillaBiomeProperties.getLoopSound() == null
                ? inherited.loop()
                : getEntry(RegistryFetcher.soundEventRegistry(), vanillaBiomeProperties.getLoopSound().location());

            builder.setAttribute(EnvironmentAttributes.AMBIENT_SOUNDS, new AmbientSounds(
                loop,
                vanillaBiomeProperties.getMoodSound() == null
                ? inherited.mood()
                : Optional.of(vanillaBiomeProperties.getMoodSound()),
                vanillaBiomeProperties.getAdditionsSound() == null
                ? inherited.additions()
                : List.of(vanillaBiomeProperties.getAdditionsSound())));
        }

        builder.hasPrecipitation(Objects.requireNonNullElse(vanillaBiomeProperties.getPrecipitation(), vanilla.hasPrecipitation()));

        builder.temperature(Objects.requireNonNullElse(vanillaBiomeProperties.getTemperature(), vanilla.getBaseTemperature()));

        builder.downfall(Objects.requireNonNullElse(vanillaBiomeProperties.getDownfall(), vanilla.climateSettings.downfall()));

        builder.temperatureAdjustment(
            Objects.requireNonNullElse(vanillaBiomeProperties.getTemperatureModifier(), vanilla.climateSettings.temperatureModifier()));

        builder.mobSpawnSettings(Objects.requireNonNullElse(vanillaBiomeProperties.getSpawnSettings(), vanilla.getMobSettings()));

        return builder
            .specialEffects(effects.build())
            .generationSettings(new BiomeGenerationSettings.PlainBuilder().build())
            .build();
    }

    public static String createBiomeID(ConfigPack pack, com.dfsek.terra.api.registry.key.RegistryKey biomeID) {
        return pack.getID()
                   .toLowerCase() + "/" + biomeID.getNamespace().toLowerCase(Locale.ROOT) + "/" + biomeID.getID().toLowerCase(Locale.ROOT);
    }
}

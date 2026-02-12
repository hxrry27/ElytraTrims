package dev.hxrry.elytratrims.config;

import dev.hxrry.elytratrims.ElytraTrims;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.EnumMap;
import java.util.Map;

public class Settings {

    public enum Effect {
        GLOW("glow", "elytratrims:glow", Material.GLOW_INK_SAC),
        COSMIC("cosmic", "elytratrims:gateway", Material.NETHER_STAR),
        ANIMATION("animation", "elytratrims:animation", Material.ENCHANTED_GOLDEN_APPLE);

        private final String configKey;
        private final String dataKey;
        private final Material defaultIngredient;

        Effect(String configKey, String dataKey, Material defaultIngredient) {
            this.configKey = configKey;
            this.dataKey = dataKey;
            this.defaultIngredient = defaultIngredient;
        }

        public String getConfigKey() { return configKey; }
        public String getDataKey() { return dataKey; }
        public Material getDefaultIngredient() { return defaultIngredient; }
    }

    private static final MiniMessage MM = MiniMessage.miniMessage();

    // feature toggles
    private final boolean trimsEnabled;
    private final boolean dyeingEnabled;
    private final boolean bannerPatternsEnabled;
    private final boolean cauldronWashingEnabled;

    // gated effects
    private final Map<Effect, EffectConfig> effectConfigs = new EnumMap<>(Effect.class);

    // messages
    private final Component noPermissionMessage;
    private final Component effectAppliedMessage;
    private final Component effectRemovedMessage;
    private final Component notElytraMessage;
    private final Component reloadMessage;

    public Settings(ElytraTrims plugin) {
        FileConfiguration config = plugin.getConfig();

        // features
        trimsEnabled = config.getBoolean("features.trims", true);
        dyeingEnabled = config.getBoolean("features.dyeing", true);
        bannerPatternsEnabled = config.getBoolean("features.banner-patterns", true);
        cauldronWashingEnabled = config.getBoolean("features.cauldron-washing", true);

        // gated effects
        for (Effect effect : Effect.values()) {
            ConfigurationSection section = config.getConfigurationSection("gated-effects." + effect.getConfigKey());
            if (section != null) {
                boolean enabled = section.getBoolean("enabled", true);
                String permission = section.getString("permission", "elytratrims.effect." + effect.getConfigKey());
                Material ingredient = parseMaterial(section.getString("ingredient"), effect.getDefaultIngredient());
                effectConfigs.put(effect, new EffectConfig(enabled, permission, ingredient));
            } else {
                effectConfigs.put(effect, new EffectConfig(true,
                        "elytratrims.effect." + effect.getConfigKey(),
                        effect.getDefaultIngredient()));
            }
        }

        // messages
        noPermissionMessage = MM.deserialize(config.getString("messages.no-permission",
                "<red>You don't have permission to apply this effect."));
        effectAppliedMessage = MM.deserialize(config.getString("messages.effect-applied",
                "<green>Effect applied to your elytra!"));
        effectRemovedMessage = MM.deserialize(config.getString("messages.effect-removed",
                "<green>Effect removed from your elytra."));
        notElytraMessage = MM.deserialize(config.getString("messages.not-elytra",
                "<red>You must be holding an elytra."));
        reloadMessage = MM.deserialize(config.getString("messages.reload",
                "<green>ElytraTrims config reloaded."));
    }

    private Material parseMaterial(String name, Material fallback) {
        if (name == null) return fallback;
        try {
            return Material.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            return fallback;
        }
    }

    // feature checks
    public boolean isTrimsEnabled() { return trimsEnabled; }
    public boolean isDyeingEnabled() { return dyeingEnabled; }
    public boolean isBannerPatternsEnabled() { return bannerPatternsEnabled; }
    public boolean isCauldronWashingEnabled() { return cauldronWashingEnabled; }

    // effect checks
    public EffectConfig getEffectConfig(Effect effect) { return effectConfigs.get(effect); }

    public boolean isEffectEnabled(Effect effect) {
        EffectConfig config = effectConfigs.get(effect);
        return config != null && config.enabled();
    }

    public boolean hasAnyGatedEffect() {
        return effectConfigs.values().stream().anyMatch(EffectConfig::enabled);
    }

    // ingr. materials
    public Effect getEffectByIngredient(Material material) {
        for (Effect effect : Effect.values()) {
            EffectConfig config = effectConfigs.get(effect);
            if (config != null && config.enabled() && config.ingredient() == material) {
                return effect;
            }
        }
        return null;
    }

    // messages
    public Component getNoPermissionMessage() { return noPermissionMessage; }
    public Component getEffectAppliedMessage() { return effectAppliedMessage; }
    public Component getEffectRemovedMessage() { return effectRemovedMessage; }
    public Component getNotElytraMessage() { return notElytraMessage; }
    public Component getReloadMessage() { return reloadMessage; }

    public record EffectConfig(boolean enabled, String permission, Material ingredient) {}
}
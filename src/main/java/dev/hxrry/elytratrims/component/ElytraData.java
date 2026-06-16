package dev.hxrry.elytratrims.component;

import dev.hxrry.elytratrims.config.Settings;
import de.tr7zw.nbtapi.NBT;
import de.tr7zw.nbtapi.iface.ReadWriteNBT;
import de.tr7zw.nbtapi.iface.ReadableNBT;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.BannerPatternLayers;
import io.papermc.paper.datacomponent.item.DyedItemColor;
import io.papermc.paper.datacomponent.item.ItemArmorTrim;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public final class ElytraData {

    private static final String KEY_GLOW = "elytratrims:glow";
    private static final String KEY_GATEWAY = "elytratrims:gateway";
    private static final String KEY_ANIMATION = "elytratrims:animation";

    private ElytraData() {}

    public static boolean isElytra(ItemStack item) {
        return item != null && item.getType() == Material.ELYTRA;
    }

    // ── Trims (DataComponentTypes.TRIM) ──

    public static boolean hasTrim(ItemStack elytra) {
        return elytra.hasData(DataComponentTypes.TRIM);
    }

    public static ItemArmorTrim getTrim(ItemStack elytra) {
        return elytra.getData(DataComponentTypes.TRIM);
    }

    public static void setTrim(ItemStack elytra, ItemArmorTrim trim) {
        elytra.setData(DataComponentTypes.TRIM, trim);
    }

    public static void removeTrim(ItemStack elytra) {
        elytra.unsetData(DataComponentTypes.TRIM);
    }

    // ── Dye Color (DataComponentTypes.DYED_COLOR) ──

    public static boolean hasDye(ItemStack elytra) {
        return elytra.hasData(DataComponentTypes.DYED_COLOR);
    }

    public static Color getDyeColor(ItemStack elytra) {
        DyedItemColor dyed = elytra.getData(DataComponentTypes.DYED_COLOR);
        return dyed != null ? dyed.color() : null;
    }

    public static void setDyeColor(ItemStack elytra, Color color) {
        elytra.setData(DataComponentTypes.DYED_COLOR, DyedItemColor.dyedItemColor().color(color).build());
    }

    public static void removeDye(ItemStack elytra) {
        elytra.unsetData(DataComponentTypes.DYED_COLOR);
    }

    // ── Banner Patterns (DataComponentTypes.BANNER_PATTERNS) ──

    public static boolean hasBannerPatterns(ItemStack elytra) {
        return elytra.hasData(DataComponentTypes.BANNER_PATTERNS);
    }

    public static BannerPatternLayers getBannerPatterns(ItemStack elytra) {
        return elytra.getData(DataComponentTypes.BANNER_PATTERNS);
    }

    public static void setBannerPatterns(ItemStack elytra, BannerPatternLayers patterns) {
        elytra.setData(DataComponentTypes.BANNER_PATTERNS, patterns);
    }

    public static void removeBannerPatterns(ItemStack elytra) {
        elytra.unsetData(DataComponentTypes.BANNER_PATTERNS);
    }

    // ── Custom Data Flags (NBTAPI → minecraft:custom_data root) ──

    public static String getKeyForEffect(Settings.Effect effect) {
        return switch (effect) {
            case GLOW -> KEY_GLOW;
            case COSMIC -> KEY_GATEWAY;
            case ANIMATION -> KEY_ANIMATION;
        };
    }

    private static final String CUSTOM_DATA = "minecraft:custom_data";

    public static boolean hasEffect(ItemStack elytra, Settings.Effect effect) {
        String key = getKeyForEffect(effect);
        return NBT.getComponents(elytra, comps -> {
            ReadableNBT customData = comps.getCompound(CUSTOM_DATA);
            return customData != null && customData.hasTag(key) && customData.getByte(key) == 1;
        });
    }

    public static void setEffect(ItemStack elytra, Settings.Effect effect, boolean value) {
        String key = getKeyForEffect(effect);
        NBT.modifyComponents(elytra, comps -> {
            if (value) {
                comps.getOrCreateCompound(CUSTOM_DATA).setByte(key, (byte) 1);
            } else if (comps.hasTag(CUSTOM_DATA)) {
                comps.getOrCreateCompound(CUSTOM_DATA).removeKey(key);
            }
        });
    }

    public static boolean hasAnyEffect(ItemStack elytra) {
        return NBT.getComponents(elytra, comps -> {
            ReadableNBT customData = comps.getCompound(CUSTOM_DATA);
            if (customData == null) return false;
            for (Settings.Effect effect : Settings.Effect.values()) {
                String key = getKeyForEffect(effect);
                if (customData.hasTag(key) && customData.getByte(key) == 1) return true;
            }
            return false;
        });
    }

    public static void removeAllEffects(ItemStack elytra) {
        NBT.modifyComponents(elytra, comps -> {
            if (!comps.hasTag(CUSTOM_DATA)) return;
            ReadWriteNBT customData = comps.getOrCreateCompound(CUSTOM_DATA);
            customData.removeKey(KEY_GLOW);
            customData.removeKey(KEY_GATEWAY);
            customData.removeKey(KEY_ANIMATION);
        });
    }

    // ── Bulk Operations ──

    public static boolean hasAnyDecoration(ItemStack elytra) {
        return hasTrim(elytra) || hasDye(elytra) || hasBannerPatterns(elytra) || hasAnyEffect(elytra);
    }

    public static void clearAllDecorations(ItemStack elytra) {
        removeTrim(elytra);
        removeDye(elytra);
        removeBannerPatterns(elytra);
        removeAllEffects(elytra);
    }
}
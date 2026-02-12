package dev.hxrry.elytratrims.component;

import dev.hxrry.elytratrims.config.Settings;
import de.tr7zw.nbtapi.NBT;
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

    public static boolean hasEffect(ItemStack elytra, Settings.Effect effect) {
        String key = getKeyForEffect(effect);
        return NBT.get(elytra, nbt -> nbt.hasTag(key) && nbt.getByte(key) == 1);
    }

    public static void setEffect(ItemStack elytra, Settings.Effect effect, boolean value) {
        String key = getKeyForEffect(effect);
        NBT.modify(elytra, nbt -> {
            if (value) {
                nbt.setByte(key, (byte) 1);
            } else {
                nbt.removeKey(key);
            }
        });
    }

    public static boolean hasAnyEffect(ItemStack elytra) {
        return NBT.get(elytra, nbt -> {
            for (Settings.Effect effect : Settings.Effect.values()) {
                String key = getKeyForEffect(effect);
                if (nbt.hasTag(key) && nbt.getByte(key) == 1) return true;
            }
            return false;
        });
    }

    public static void removeAllEffects(ItemStack elytra) {
        NBT.modify(elytra, nbt -> {
            nbt.removeKey(KEY_GLOW);
            nbt.removeKey(KEY_GATEWAY);
            nbt.removeKey(KEY_ANIMATION);
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
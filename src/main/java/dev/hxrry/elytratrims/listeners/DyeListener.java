package dev.hxrry.elytratrims.listeners;

import dev.hxrry.elytratrims.ElytraTrims;
import dev.hxrry.elytratrims.component.ElytraData;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class DyeListener implements Listener {

    @SuppressWarnings("unused")
    private final ElytraTrims plugin;

    public DyeListener(ElytraTrims plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPrepareCraft(PrepareItemCraftEvent event) {
        CraftingInventory inv = event.getInventory();

        ItemStack elytra = null;
        List<Color> dyeColors = new ArrayList<>();

        for (ItemStack item : inv.getMatrix()) {
            if (item == null || item.getType().isAir()) continue;

            if (ElytraData.isElytra(item)) {
                if (elytra != null) return;
                elytra = item;
            } else {
                Color dye = getDyeColor(item.getType());
                if (dye != null) {
                    dyeColors.add(dye);
                } else {
                    return;
                }
            }
        }

        if (elytra == null || dyeColors.isEmpty()) return;

        if (event.getView().getPlayer() instanceof Player player) {
            if (!player.hasPermission("elytratrims.craft.dye")) {
                inv.setResult(null);
                return;
            }
        }

        Color blended = blendColors(elytra, dyeColors);

        ItemStack result = elytra.clone();
        result.setAmount(1);
        ElytraData.setDyeColor(result, blended);
        inv.setResult(result);
    }

    private Color blendColors(ItemStack elytra, List<Color> dyeColors) {
        int totalR = 0, totalG = 0, totalB = 0;
        int totalMax = 0;
        int count = 0;

        if (ElytraData.hasDye(elytra)) {
            Color existing = ElytraData.getDyeColor(elytra);
            if (existing != null) {
                totalR += existing.getRed();
                totalG += existing.getGreen();
                totalB += existing.getBlue();
                totalMax += Math.max(existing.getRed(), Math.max(existing.getGreen(), existing.getBlue()));
                count++;
            }
        }

        for (Color dye : dyeColors) {
            totalR += dye.getRed();
            totalG += dye.getGreen();
            totalB += dye.getBlue();
            totalMax += Math.max(dye.getRed(), Math.max(dye.getGreen(), dye.getBlue()));
            count++;
        }

        if (count == 0) return Color.WHITE;

        int avgR = totalR / count;
        int avgG = totalG / count;
        int avgB = totalB / count;
        float avgMax = (float) totalMax / count;

        float maxAvg = Math.max(avgR, Math.max(avgG, avgB));
        if (maxAvg == 0) return Color.fromRGB(avgR, avgG, avgB);

        float scale = avgMax / maxAvg;
        return Color.fromRGB(
                Math.min(255, Math.round(avgR * scale)),
                Math.min(255, Math.round(avgG * scale)),
                Math.min(255, Math.round(avgB * scale))
        );
    }


    private Color getDyeColor(Material material) {
        return switch (material) {
            case WHITE_DYE -> Color.fromRGB(249, 255, 254);
            case ORANGE_DYE -> Color.fromRGB(249, 128, 29);
            case MAGENTA_DYE -> Color.fromRGB(199, 78, 189);
            case LIGHT_BLUE_DYE -> Color.fromRGB(58, 179, 218);
            case YELLOW_DYE -> Color.fromRGB(254, 216, 61);
            case LIME_DYE -> Color.fromRGB(128, 199, 31);
            case PINK_DYE -> Color.fromRGB(243, 139, 170);
            case GRAY_DYE -> Color.fromRGB(71, 79, 82);
            case LIGHT_GRAY_DYE -> Color.fromRGB(157, 157, 151);
            case CYAN_DYE -> Color.fromRGB(22, 156, 156);
            case PURPLE_DYE -> Color.fromRGB(137, 50, 184);
            case BLUE_DYE -> Color.fromRGB(60, 68, 170);
            case BROWN_DYE -> Color.fromRGB(131, 84, 50);
            case GREEN_DYE -> Color.fromRGB(94, 124, 22);
            case RED_DYE -> Color.fromRGB(176, 46, 38);
            case BLACK_DYE -> Color.fromRGB(29, 29, 33);
            default -> null;
        };
    }
}